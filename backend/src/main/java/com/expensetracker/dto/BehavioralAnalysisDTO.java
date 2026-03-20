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
public class BehavioralAnalysisDTO {
    private int dailyTransactionCount;
    private String peakSpendingDay;
    private String peakSpendingHour;
    private double weekdayAvgSpending;
    private double weekendAvgSpending;
    private int impulseTransactionCount;
    private List<SavingTipDTO> tips;
}
