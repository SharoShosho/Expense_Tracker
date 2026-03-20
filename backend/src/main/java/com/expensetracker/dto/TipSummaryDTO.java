package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipSummaryDTO {
    private String type;
    private String title;
    private String summary;
    private String priority;
    private double potentialSavings;
}
