package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAnalysisDTO {
    private String categoryName;
    private double totalSpent;
    private double budget;
    private double budgetUsagePercent;
    private int transactionCount;
    private double avgTransactionAmount;
    private double maxTransactionAmount;
    private double minTransactionAmount;
    private Map<String, Double> spendingByWeek;
    private List<SavingTipDTO> tips;
}
