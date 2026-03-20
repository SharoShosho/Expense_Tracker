package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.model.UserBehaviorProfile;
import com.expensetracker.model.UserTipInteraction;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserBehaviorProfileRepository;
import com.expensetracker.repository.UserTipInteractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages per-user behavioral profiles and personalization scores.
 *
 * <p>Profiles are stored in MongoDB and auto-created for new users on first
 * access.  Engagement scores are updated whenever the user provides feedback
 * via {@link #updatePersonalizationScores}.
 */
@Service
public class PersonalizationEngine {

    private static final Logger log = LoggerFactory.getLogger(PersonalizationEngine.class);

    // Default tip types used to initialize personalization scores
    private static final List<String> TIP_TYPES = List.of(
            "SPENDING_PATTERN", "BEHAVIORAL", "BENCHMARKING",
            "PREDICTIONS", "ANOMALIES", "CATEGORY", "WELLNESS", "HISTORY"
    );

    // Budget adherence thresholds for spending-pattern detection
    private static final double CONSERVATIVE_THRESHOLD = 0.70;
    private static final double MODERATE_THRESHOLD     = 0.90;

    @Autowired
    private UserBehaviorProfileRepository profileRepository;

    @Autowired
    private UserTipInteractionRepository interactionRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Value("${nn.personalization.learning-phase-count:30}")
    private int learningPhaseCount;

    @Value("${nn.personalization.score-increase:0.1}")
    private double scoreIncrease;

    @Value("${nn.personalization.score-decrease:0.05}")
    private double scoreDecrease;

    @Value("${nn.personalization.score-min:0.2}")
    private double scoreMin;

    @Value("${nn.personalization.score-max:1.0}")
    private double scoreMax;

    @Value("${nn.personalization.multiplier-min:0.5}")
    private double multiplierMin;

    @Value("${nn.personalization.multiplier-max:1.5}")
    private double multiplierMax;

    @Value("${nn.personalization.risk-conservative-max:33}")
    private int riskConservativeMax;

    @Value("${nn.personalization.risk-moderate-max:66}")
    private int riskModerateMax;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the existing profile for {@code userId}, or creates and persists
     * a new default profile if none exists yet.
     */
    public UserBehaviorProfile loadOrCreateProfile(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> initializeUserProfile(userId));
    }

    /**
     * Creates and persists a brand-new default profile.
     * Called automatically by {@link #loadOrCreateProfile} for new users.
     */
    public UserBehaviorProfile initializeUserProfile(String userId) {
        Map<String, Double> initialScores = new HashMap<>();
        TIP_TYPES.forEach(type -> initialScores.put(type, 1.0));

        UserBehaviorProfile profile = UserBehaviorProfile.builder()
                .userId(userId)
                .spendingPattern("MODERATE")
                .riskTolerance(50)
                .preferredCategories(new ArrayList<>())
                .ignoreCategories(new ArrayList<>())
                .seasonalPatterns(new HashMap<>())
                .tipEngagementScores(initialScores)
                .tipsFollowed(0)
                .tipsIgnored(0)
                .learningPhase(true)
                .createdAt(LocalDateTime.now())
                .build();

        UserBehaviorProfile saved = profileRepository.save(profile);
        log.debug("Initialized behavior profile for user {}", userId);
        return saved;
    }

    /**
     * Analyses the last 6 months of spending versus budgets and updates the
     * profile's {@code spendingPattern} field (CONSERVATIVE / MODERATE / AGGRESSIVE).
     */
    public void detectSpendingPattern(String userId) {
        UserBehaviorProfile profile = loadOrCreateProfile(userId);

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(
                userId, sixMonthsAgo, LocalDate.now());
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);

        double totalSpent  = expenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                .sum();
        double totalBudget = budgets.stream()
                .mapToDouble(b -> b.getAmount() != null ? b.getAmount().doubleValue() : 0.0)
                .sum() * 6; // convert monthly budget to match 6-month window

        String pattern;
        if (totalBudget <= 0) {
            pattern = "MODERATE"; // not enough data
        } else {
            double ratio = totalSpent / totalBudget;
            if (ratio < CONSERVATIVE_THRESHOLD) {
                pattern = "CONSERVATIVE";
            } else if (ratio < MODERATE_THRESHOLD) {
                pattern = "MODERATE";
            } else {
                pattern = "AGGRESSIVE";
            }
        }

        profile.setSpendingPattern(pattern);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        log.debug("Detected spending pattern {} for user {}", pattern, userId);
    }

    /**
     * Calculates a 0–100 risk-tolerance score based on how consistently the
     * user stays within their budgets over the last 6 months.
     *
     * <ul>
     *   <li>0–33  → budget-conscious (CONSERVATIVE)</li>
     *   <li>34–66 → balanced (MODERATE)</li>
     *   <li>67–100 → carefree spender (AGGRESSIVE)</li>
     * </ul>
     */
    public void detectRiskTolerance(String userId) {
        UserBehaviorProfile profile = loadOrCreateProfile(userId);

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(
                userId, sixMonthsAgo, LocalDate.now());
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);

        double totalSpent  = expenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                .sum();
        double totalBudget = budgets.stream()
                .mapToDouble(b -> b.getAmount() != null ? b.getAmount().doubleValue() : 0.0)
                .sum() * 6;

        int riskScore;
        if (totalBudget <= 0) {
            riskScore = 50;
        } else {
            // ratio > 1.0 means over-budget; clamp to [0, 2]
            double ratio = Math.min(totalSpent / totalBudget, 2.0);
            // Map [0..2] → [0..100]: low ratio = low risk, high ratio = high risk
            riskScore = (int) Math.round(ratio * 50.0);
        }

        profile.setRiskTolerance(riskScore);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        log.debug("Detected risk tolerance {} for user {}", riskScore, userId);
    }

    /**
     * Updates the personalization engagement score for {@code tipType} based
     * on whether the user followed the tip or not.
     *
     * <p>Score change:
     * <ul>
     *   <li>Followed  → score += {@code nn.personalization.score-increase} (0.1)</li>
     *   <li>Ignored   → score −= {@code nn.personalization.score-decrease} (0.05)</li>
     *   <li>Clamped to [{@code nn.personalization.score-min}, {@code nn.personalization.score-max}]</li>
     * </ul>
     */
    public void updatePersonalizationScores(String userId, String tipType, boolean wasFollowed) {
        if (tipType == null || tipType.isBlank()) return;

        UserBehaviorProfile profile = loadOrCreateProfile(userId);
        Map<String, Double> scores = new HashMap<>(profile.getTipEngagementScores());

        double current = scores.getOrDefault(tipType, 1.0);
        double updated = wasFollowed ? current + scoreIncrease : current - scoreDecrease;
        updated = Math.max(scoreMin, Math.min(scoreMax, updated));
        scores.put(tipType, updated);

        profile.setTipEngagementScores(scores);
        if (wasFollowed) {
            profile.setTipsFollowed(profile.getTipsFollowed() + 1);
            profile.setLastTipFollowedDate(LocalDateTime.now());
        } else {
            profile.setTipsIgnored(profile.getTipsIgnored() + 1);
        }

        int totalInteractions = profile.getTipsFollowed() + profile.getTipsIgnored();
        profile.setLearningPhase(totalInteractions < learningPhaseCount);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        log.debug("Updated personalization score for user {} tipType {}: {} → {}",
                userId, tipType, current, updated);
    }

    /**
     * Returns a map of tip-type → personalization multiplier in the range
     * [{@code nn.personalization.multiplier-min}, {@code nn.personalization.multiplier-max}].
     *
     * <p>Multipliers are derived from engagement scores via linear interpolation.
     */
    public Map<String, Double> getPersonalizationMultipliers(String userId) {
        UserBehaviorProfile profile = loadOrCreateProfile(userId);
        Map<String, Double> multipliers = new LinkedHashMap<>();

        for (String tipType : TIP_TYPES) {
            double engagementScore = profile.getTipEngagementScores().getOrDefault(tipType, 1.0);
            // Linear interpolation: score in [scoreMin, scoreMax] → multiplier in [multiplierMin, multiplierMax]
            double range = scoreMax - scoreMin;
            double multiplierRange = multiplierMax - multiplierMin;
            double normalised = range > 0 ? (engagementScore - scoreMin) / range : 0.5;
            double multiplier = multiplierMin + normalised * multiplierRange;
            multiplier = Math.max(multiplierMin, Math.min(multiplierMax, multiplier));
            multipliers.put(tipType, Math.round(multiplier * 1000.0) / 1000.0);
        }

        return multipliers;
    }

    /**
     * Returns user-specific NN confidence thresholds adjusted for the user's
     * risk tolerance.  Conservative users require higher confidence before a
     * tip is surfaced; aggressive spenders are shown tips at lower confidence.
     *
     * @return map of threshold label → value
     */
    public Map<String, Double> getRecommendedThresholds(String userId) {
        UserBehaviorProfile profile = loadOrCreateProfile(userId);
        int risk = profile.getRiskTolerance();

        // Offset: CONSERVATIVE shifts thresholds up, AGGRESSIVE shifts them down
        double offset = 0.0;
        if (risk <= riskConservativeMax) {
            offset = 0.1;   // need more confidence for conservative users
        } else if (risk > riskModerateMax) {
            offset = -0.1;  // show tips at lower confidence for aggressive spenders
        }

        Map<String, Double> thresholds = new LinkedHashMap<>();
        thresholds.put("low",  clamp(0.4 + offset, 0.2, 0.8));
        thresholds.put("med",  clamp(0.6 + offset, 0.3, 0.9));
        thresholds.put("high", clamp(0.8 + offset, 0.5, 1.0));
        return thresholds;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
