package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthPredictionDTO {
    private String month;
    private double predictedAmount;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String recommendation;
}

