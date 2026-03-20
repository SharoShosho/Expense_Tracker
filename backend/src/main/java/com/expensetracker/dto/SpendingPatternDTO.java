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
public class SpendingPatternDTO {
    private double totalMonthlySpending;
    private double totalBudget;
    private double budgetUsagePercent;
    private List<CategorySpendingDTO> categories;
    private List<String> overBudgetCategories;
    private double totalPotentialSavings;
    private List<SavingTipDTO> tips;
}
