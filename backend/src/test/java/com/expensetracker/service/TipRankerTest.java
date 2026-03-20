package com.expensetracker.service;

import com.expensetracker.dto.SavingTipDTO;
import com.expensetracker.model.UserBehaviorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TipRankerTest {

    private TipRanker ranker;

    @BeforeEach
    void setUp() {
        ranker = new TipRanker();
        // userBehaviorProfileRepository left null (optional dependency)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // calculateTipScore
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void calculateTipScore_highPriorityWithHighConfidence() {
        SavingTipDTO tip = buildTip("HIGH");
        double score = ranker.calculateTipScore(tip, 1.0);
        // base=80, boost=20 → (80+20)/2 = 50   (clamped to ≤100)
        assertEquals(50.0, score, 0.01);
    }

    @Test
    void calculateTipScore_lowPriorityWithZeroConfidence() {
        SavingTipDTO tip = buildTip("LOW");
        double score = ranker.calculateTipScore(tip, 0.0);
        // base=40, boost=0 → (40+0)/2 = 20
        assertEquals(20.0, score, 0.01);
    }

    @Test
    void calculateTipScore_mediumPriorityMidConfidence() {
        SavingTipDTO tip = buildTip("MEDIUM");
        double score = ranker.calculateTipScore(tip, 0.5);
        // base=60, boost=10 → (60+10)/2 = 35
        assertEquals(35.0, score, 0.01);
    }

    @Test
    void calculateTipScore_clampedToMax100() {
        SavingTipDTO tip = buildTip("HIGH");
        double score = ranker.calculateTipScore(tip, 100.0); // absurdly high confidence
        assertTrue(score <= 100.0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rankTips
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void rankTips_higherPriorityComesFirst() {
        List<SavingTipDTO> ruleTips = List.of(buildTip("LOW", "Low tip"), buildTip("HIGH", "High tip"));
        List<SavingTipDTO> nnTips   = List.of();
        double[] nnOutput = {0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9};

        List<SavingTipDTO> ranked = ranker.rankTips(ruleTips, nnTips, nnOutput);
        assertEquals(2, ranked.size());
        assertEquals("High tip", ranked.get(0).getTitle());
    }

    @Test
    void rankTips_returnsAllTipsWhenNoNNOutput() {
        List<SavingTipDTO> ruleTips = List.of(buildTip("HIGH", "T1"), buildTip("MEDIUM", "T2"));
        List<SavingTipDTO> ranked = ranker.rankTips(ruleTips, List.of(), null);
        assertEquals(2, ranked.size());
    }

    @Test
    void rankTips_combinesRuleAndNNTips() {
        List<SavingTipDTO> ruleTips = List.of(buildTip("HIGH", "Rule tip"));
        List<SavingTipDTO> nnTips   = List.of(buildTip("HIGH", "NN tip"));
        double[] nnOutput = {0.85};

        List<SavingTipDTO> ranked = ranker.rankTips(ruleTips, nnTips, nnOutput);
        assertEquals(2, ranked.size());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // filterByRelevance
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void filterByRelevance_removeDuplicateTitles() {
        List<SavingTipDTO> tips = List.of(
                buildTip("HIGH", "Same title"),
                buildTip("HIGH", "same title"), // duplicate (case-insensitive)
                buildTip("LOW",  "Unique title"));

        List<SavingTipDTO> filtered = ranker.filterByRelevance(tips);
        assertEquals(2, filtered.size());
    }

    @Test
    void filterByRelevance_returnsAtMostTenTips() {
        List<SavingTipDTO> manyTips = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            manyTips.add(buildTip("HIGH", "Tip " + i));
        }
        List<SavingTipDTO> filtered = ranker.filterByRelevance(manyTips);
        assertTrue(filtered.size() <= 10);
    }

    @Test
    void filterByRelevance_emptyInputReturnsEmpty() {
        assertTrue(ranker.filterByRelevance(List.of()).isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // personalizeRanking
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void personalizeRanking_nullProfileReturnsInputUnchanged() {
        List<SavingTipDTO> tips = List.of(buildTip("HIGH", "T1"), buildTip("LOW", "T2"));
        List<SavingTipDTO> result = ranker.personalizeRanking(tips, null);
        assertEquals(tips, result);
    }

    @Test
    void personalizeRanking_boostedCategoryRisesInRanking() {
        // Create a profile that strongly prefers HISTORY tips
        UserBehaviorProfile profile = UserBehaviorProfile.builder()
                .userId("u1")
                .tipEngagementScores(Map.of("HISTORY", 1.5, "SPENDING_PATTERN", 0.5))
                .build();

        List<SavingTipDTO> tips = List.of(
                buildTip("LOW",  "History Trend Alert"),     // HISTORY type → boosted
                buildTip("HIGH", "Budget Exceeded tip"));    // SPENDING_PATTERN type

        List<SavingTipDTO> ranked = ranker.personalizeRanking(tips, profile);
        assertEquals(2, ranked.size());
        // LOW history tip (40 × 1.5 = 60) > HIGH spending-pattern (80 × 0.5 = 40)
        assertEquals("History Trend Alert", ranked.get(0).getTitle());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rankFilterAndPersonalize (full pipeline)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void rankFilterAndPersonalize_returnsNonEmptyList() {
        List<SavingTipDTO> ruleTips = List.of(buildTip("HIGH", "Rule Tip A"), buildTip("MEDIUM", "Rule Tip B"));
        double[] nnOutput = {0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};

        List<SavingTipDTO> result = ranker.rankFilterAndPersonalize("u-test", ruleTips, List.of(), nnOutput);
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 10);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UserBehaviorProfile.getPreferenceMultiplier
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void userBehaviorProfile_getPreferenceMultiplier_clampsToRange() {
        UserBehaviorProfile profile = UserBehaviorProfile.builder()
                .userId("u1")
                .tipEngagementScores(Map.of("X", 10.0, "Y", -5.0))
                .build();

        assertEquals(1.5, profile.getPreferenceMultiplier("X"), 0.001);
        assertEquals(0.5, profile.getPreferenceMultiplier("Y"), 0.001);
        assertEquals(1.0, profile.getPreferenceMultiplier("UNKNOWN"), 0.001);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private SavingTipDTO buildTip(String priority) {
        return buildTip(priority, "Generic Tip Title");
    }

    private SavingTipDTO buildTip(String priority, String title) {
        return SavingTipDTO.builder()
                .title(title)
                .message("Test message for " + title)
                .priority(priority)
                .potentialSavings(0)
                .actionItems(List.of("Take action"))
                .build();
    }
}
