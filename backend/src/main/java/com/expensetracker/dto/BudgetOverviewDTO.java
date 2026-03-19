package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class BudgetOverviewDTO {
    private String month;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private int nearLimitCount;
    private int exceededCount;
    private List<BudgetStatusDTO> categories;
}

