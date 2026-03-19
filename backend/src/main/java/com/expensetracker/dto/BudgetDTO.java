package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetDTO {

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Budget amount is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal amount;
}

