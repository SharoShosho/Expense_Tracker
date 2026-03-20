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
public class TipsOverviewDTO {
    private int totalTipsCount;
    private double totalPotentialSavings;
    private int wellnessScore;
    private List<TipSummaryDTO> summaries;
}
