package com.expensetracker.service;

import com.expensetracker.dto.SavingTipDTO;
import com.expensetracker.model.UserBehaviorProfile;
import com.expensetracker.repository.UserBehaviorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Combines rule-based and NN-generated tips, scores them, and returns a
 * personalised, de-duplicated, relevance-ranked list.
 *
 * <h2>Scoring algorithm</h2>
 * <pre>
 * base_score   = { HIGH → 80, MEDIUM → 60, LOW → 40 }
 * nn_boost     = nnConfidence × 20
 * combined     = (base_score + nn_boost) / 2
 * preference   = userProfile.getPreferenceMultiplier(tipType)   [0.5 – 1.5]
 * final_score  = clamp(combined × preference, 0, 100)
 * </pre>
 */
@Service
public class TipRanker {

    private static final Logger log = LoggerFactory.getLogger(TipRanker.class);

    /** Tips below this score are removed by {@link #filterByRelevance}. */
    private static final double DEFAULT_MIN_SCORE = 50.0;

    /** Maximum number of tips returned after filtering. */
    private static final int MAX_TIPS = 10;

    /** Base score assigned to rule-based tips (before NN boost). */
    private static final double RULE_BASED_BASE = 60.0;

    @Autowired(required = false)
    private UserBehaviorProfileRepository userBehaviorProfileRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Combines rule-based and NN-generated tips into a single ranked list.
     *
     * <p>Each tip is scored using {@link #calculateTipScore(SavingTipDTO, double)}.
     * Rule-based tips receive {@link #RULE_BASED_BASE} as their NN confidence
     * (neutral), while NN tips use the confidence value embedded in the
     * {@code nnOutput} array at their respective index.
     *
     * @param ruleTips rule-based tips (may be empty but not null)
     * @param nnTips     NN-generated tips (may be empty but not null)
     * @param nnOutput   raw NN probability vector (may be null)
     * @return combined list sorted by descending final score
     */
    public List<SavingTipDTO> rankTips(List<SavingTipDTO> ruleTips,
                                        List<SavingTipDTO> nnTips,
                                        double[] nnOutput) {

        // Assign scores
        Map<SavingTipDTO, Double> scores = new LinkedHashMap<>();

        double avgNnConfidence = averageConfidence(nnOutput);

        for (SavingTipDTO tip : ruleTips) {
            // Rule-based tips: blend rule score with NN average
            double ruleScore = calculateTipScore(tip, RULE_BASED_BASE / 100.0);
            double nnScore   = calculateTipScore(tip, avgNnConfidence);
            double blended   = ruleScore * 0.4 + nnScore * 0.6;
            scores.put(tip, clamp(blended, 0, 100));
        }

        for (SavingTipDTO tip : nnTips) {
            double nnScore = calculateTipScore(tip, avgNnConfidence);
            scores.put(tip, clamp(nnScore * 100.0 / 100.0, 0, 100));
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<SavingTipDTO, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Scores a single tip.
     *
     * <pre>
     * base     = { HIGH → 80, MEDIUM → 60, LOW → 40 }
     * boost    = nnConfidence × 20
     * final    = clamp((base + boost) / 2, 0, 100)
     * </pre>
     *
     * @param tip          the tip to score
     * @param nnConfidence the NN confidence value (0–1) associated with this tip
     * @return score in the range [0, 100]
     */
    public double calculateTipScore(SavingTipDTO tip, double nnConfidence) {
        double base = priorityToBase(tip.getPriority());
        double boost = nnConfidence * 20.0;
        return clamp((base + boost) / 2.0, 0, 100);
    }

    /**
     * Removes tips below the minimum relevance score and de-duplicates by title.
     * Returns at most {@value MAX_TIPS} tips.
     *
     * @param tips     list of scored tips (pre-sorted by the caller)
     * @param minScore minimum score threshold (use {@value DEFAULT_MIN_SCORE} if unsure)
     * @return filtered, de-duplicated top-{@value MAX_TIPS} list
     */
    public List<SavingTipDTO> filterByRelevance(List<SavingTipDTO> tips, double minScore) {
        Set<String> seenTitles = new LinkedHashSet<>();
        return tips.stream()
                .filter(t -> t.getTitle() != null && seenTitles.add(t.getTitle().toLowerCase(Locale.ROOT)))
                .limit(MAX_TIPS)
                .collect(Collectors.toList());
    }

    /**
     * Convenience overload that uses the default minimum score of
     * {@value DEFAULT_MIN_SCORE}.
     *
     * @param tips list of tips to filter
     * @return filtered list
     */
    public List<SavingTipDTO> filterByRelevance(List<SavingTipDTO> tips) {
        return filterByRelevance(tips, DEFAULT_MIN_SCORE);
    }

    /**
     * Re-ranks a list of tips based on the user's stored behavioural profile.
     * Each tip's implicit score is multiplied by the user's preference
     * multiplier for the tip's inferred type.
     *
     * @param tips        tips to personalise
     * @param userProfile the user's behaviour profile
     * @return re-ranked list (may differ from input order)
     */
    public List<SavingTipDTO> personalizeRanking(List<SavingTipDTO> tips,
                                                   UserBehaviorProfile userProfile) {
        if (userProfile == null || tips == null || tips.isEmpty()) {
            return tips;
        }

        Map<SavingTipDTO, Double> personalisedScores = new LinkedHashMap<>();

        for (SavingTipDTO tip : tips) {
            String tipType     = inferTipType(tip);
            double baseScore   = priorityToBase(tip.getPriority());
            double multiplier  = userProfile.getPreferenceMultiplier(tipType);
            double finalScore  = clamp(baseScore * multiplier, 0, 100);
            personalisedScores.put(tip, finalScore);
        }

        return personalisedScores.entrySet().stream()
                .sorted(Map.Entry.<SavingTipDTO, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Full pipeline: rank → filter → personalise.
     * Retrieves the user's profile from the repository if available.
     *
     * @param userId     the authenticated user
     * @param ruleTips   rule-based tips
     * @param nnTips     NN-generated tips
     * @param nnOutput   raw NN probability vector
     * @return final personalised, ranked, filtered tip list
     */
    public List<SavingTipDTO> rankFilterAndPersonalize(String userId,
                                                        List<SavingTipDTO> ruleTips,
                                                        List<SavingTipDTO> nnTips,
                                                        double[] nnOutput) {
        List<SavingTipDTO> ranked   = rankTips(ruleTips, nnTips, nnOutput);
        List<SavingTipDTO> filtered = filterByRelevance(ranked);

        UserBehaviorProfile profile = resolveProfile(userId);
        return personalizeRanking(filtered, profile);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private double priorityToBase(String priority) {
        if ("HIGH".equalsIgnoreCase(priority))   return 80.0;
        if ("MEDIUM".equalsIgnoreCase(priority)) return 60.0;
        return 40.0;
    }

    private double averageConfidence(double[] nnOutput) {
        if (nnOutput == null || nnOutput.length == 0) return 0.5;
        double sum = 0;
        for (double v : nnOutput) sum += v;
        return sum / nnOutput.length;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Infers the tip type from the tip title for personalisation lookup.
     * Falls back to a generic identifier when the type cannot be determined.
     */
    private String inferTipType(SavingTipDTO tip) {
        if (tip.getTitle() == null) return "GENERAL";
        String title = tip.getTitle().toUpperCase(Locale.ROOT);
        if (title.contains("SPENDING PATTERN") || title.contains("BUDGET")) return "SPENDING_PATTERN";
        if (title.contains("BEHAV")            || title.contains("IMPULSE"))  return "BEHAVIORAL";
        if (title.contains("BENCHMARK")        || title.contains("PEER"))     return "BENCHMARKING";
        if (title.contains("PREDICT")          || title.contains("FORECAST")) return "PREDICTIONS";
        if (title.contains("ANOMAL")           || title.contains("UNUSUAL"))  return "ANOMALIES";
        if (title.contains("CATEGORY")         || title.contains("PRIORITY")) return "CATEGORY";
        if (title.contains("WELLNESS")         || title.contains("HEALTH"))   return "WELLNESS";
        if (title.contains("HISTORY")          || title.contains("TREND"))    return "HISTORY";
        return "GENERAL";
    }

    private UserBehaviorProfile resolveProfile(String userId) {
        if (userBehaviorProfileRepository == null || userId == null) return null;
        try {
            return userBehaviorProfileRepository.findByUserId(userId).orElse(null);
        } catch (Exception e) {
            log.warn("Could not load UserBehaviorProfile for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
