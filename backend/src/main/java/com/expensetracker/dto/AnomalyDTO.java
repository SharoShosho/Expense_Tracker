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
public class AnomalyDTO {
    private int anomalyCount;
    private double totalAnomalyAmount;
    private List<AnomalyItemDTO> anomalies;
    private List<SavingTipDTO> tips;
}
