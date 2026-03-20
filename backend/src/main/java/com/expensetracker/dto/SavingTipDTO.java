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
public class SavingTipDTO {
    private String title;
    private String message;
    private String priority; // HIGH, MEDIUM, LOW
    private double potentialSavings;
    private List<String> actionItems;
}
