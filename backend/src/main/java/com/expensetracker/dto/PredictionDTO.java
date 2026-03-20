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
public class PredictionDTO {
    private double currentMonthSpending;
    private double trendPercent;
    private List<MonthPredictionDTO> predictions;
    private List<SavingTipDTO> tips;
}
