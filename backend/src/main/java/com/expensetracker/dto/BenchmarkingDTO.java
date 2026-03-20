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
public class BenchmarkingDTO {
    private Map<String, Double> userSpendingByCategory;
    private Map<String, Double> avgSpendingByCategory;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<SavingTipDTO> tips;
}
