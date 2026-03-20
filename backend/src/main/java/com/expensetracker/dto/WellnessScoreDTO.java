package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessScoreDTO {
    private int overallScore;
    private int spendingDisciplineScore;
    private int budgetAdherenceScore;
    private int savingRateScore;
    private int financialAwarenessScore;
    private int riskManagementScore;
    private String scoreLabel; // POOR, FAIR, GOOD, EXCELLENT
    private String nextMilestone;
    private int pointsToNextMilestone;
    private List<SavingTipDTO> tips;
}
