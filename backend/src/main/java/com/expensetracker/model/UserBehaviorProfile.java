package com.expensetracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Persists per-user behavioural signals used to personalise tip ranking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_behavior_profiles")
public class UserBehaviorProfile {

    @Id
    private String id;

    /** One profile per user. */
    @Indexed(unique = true)
    private String userId;

    /** Overall spending disposition: CONSERVATIVE, MODERATE, AGGRESSIVE. */
    @Builder.Default
    private String spendingPattern = "MODERATE";

    /** 0-100: how much the user cares about overspending alerts. */
    @Builder.Default
    private int riskTolerance = 50;

    /** Categories the user actively acts on. */
    @Builder.Default
    private List<String> preferredCategories = List.of();

    /** Categories whose advice the user consistently ignores. */
    @Builder.Default
    private List<String> ignoreCategories = List.of();

    /**
     * Month-of-year (1–12) → spending multiplier relative to annual average.
     * A value > 1.0 indicates a historically expensive month.
     */
    @Builder.Default
    private Map<String, Double> seasonalPatterns = new HashMap<>();

    /**
     * Tip type → engagement score (0.0–2.0).
     * Values > 1.0 mean the user responded positively; < 1.0 means ignored.
     */
    @Builder.Default
    private Map<String, Double> tipEngagementScores = new HashMap<>();

    /** Cumulative count of tips the user has followed. */
    @Builder.Default
    private int tipsFollowed = 0;

    /** Cumulative count of tips the user has ignored. */
    @Builder.Default
    private int tipsIgnored = 0;

    /** Timestamp of the last tip the user marked as followed. */
    private LocalDateTime lastTipFollowedDate;

    /**
     * When {@code true} the system is still gathering enough feedback to
     * make meaningful personalisation decisions.
     * See {@code nn.personalization.learning-phase-count}.
     */
    @Builder.Default
    private boolean learningPhase = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // ──────────────────────────────────────────────────────────────────────────
    // Helper used by TipRanker
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns a multiplier (0.5–1.5) that reflects how receptive this user
     * historically is to a particular tip type.
     *
     * @param tipType one of the 8 tip-type identifiers (e.g. "SPENDING_PATTERN")
     * @return multiplier in the range [0.5, 1.5]
     */
    public double getPreferenceMultiplier(String tipType) {
        double raw = tipEngagementScores.getOrDefault(tipType, 1.0);
        // Clamp to [0.5, 1.5]
        return Math.max(0.5, Math.min(1.5, raw));
    }
}
