package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyItemDTO {
    private String description;
    private String category;
    private double amount;
    private double averageForCategory;
    private double deviationPercent;
    private LocalDate date;
    private String suggestion;
}
