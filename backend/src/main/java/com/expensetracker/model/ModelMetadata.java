package com.expensetracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "model_metadata")
public class ModelMetadata {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String modelVersion;

    @Builder.Default
    private TrainingStatus trainingStatus = TrainingStatus.PENDING;

    private LocalDateTime trainingStartTime;

    private LocalDateTime trainingEndTime;

    private double accuracy;

    private double lossFunction;

    private int totalSamples;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private String modelPath;
}
