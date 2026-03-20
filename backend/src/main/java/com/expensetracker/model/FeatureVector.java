package com.expensetracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Holds a single training sample: 20 input features and 8 output labels.
 * Features and labels are described in FeatureExtractionService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureVector {

    private String userId;

    /** 20 numeric features extracted from one calendar month of expenses. */
    private double[] features;

    /** 8 binary labels — one per tip type (1.0 = tip applies, 0.0 = does not apply). */
    private double[] labels;

    private LocalDateTime timestamp;
}
