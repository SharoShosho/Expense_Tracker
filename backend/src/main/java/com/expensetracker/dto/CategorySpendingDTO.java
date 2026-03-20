package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpendingDTO {
    private String category;
    private BigDecimal amount;
    private double percentage;
    private BigDecimal budget;
    private String status; // SAFE, NEAR_LIMIT, EXCEEDED, NO_BUDGET
    private double overBudgetAmount;
}
