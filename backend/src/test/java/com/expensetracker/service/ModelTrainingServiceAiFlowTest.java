package com.expensetracker.service;

import com.expensetracker.model.FeatureVector;
import com.expensetracker.model.ModelMetadata;
import com.expensetracker.model.TrainingStatus;
import com.expensetracker.nn.NeuralNetworkModel;
import com.expensetracker.repository.ModelMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ModelTrainingServiceAiFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void trainAndLoadModelWorksForEndToEndAiFlow() {
        String userId = "u-ai-e2e";
        FeatureExtractionService featureExtractionService = Mockito.mock(FeatureExtractionService.class);
        ModelMetadataRepository repository = Mockito.mock(ModelMetadataRepository.class);

        List<FeatureVector> dataset = createDataset(userId, 6, 1.2);
        when(featureExtractionService.createDataSet(eq(userId))).thenReturn(dataset);
        when(featureExtractionService.normalizeFeatures(any(double[][].class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<ModelMetadata> latest = new AtomicReference<>();
        when(repository.save(any(ModelMetadata.class))).thenAnswer(invocation -> {
            ModelMetadata metadata = invocation.getArgument(0);
            if (metadata.getId() == null) {
                metadata.setId(UUID.randomUUID().toString());
            }
            latest.set(metadata);
            return metadata;
        });
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(eq(userId))).thenAnswer(invocation -> Optional.ofNullable(latest.get()));
        when(repository.findAllByUserIdOrderByCreatedAtDesc(eq(userId))).thenAnswer(invocation -> {
            ModelMetadata current = latest.get();
            return current == null ? List.of() : List.of(current);
        });

        ModelTrainingService service = new ModelTrainingService();
        ReflectionTestUtils.setField(service, "featureExtractionService", featureExtractionService);
        ReflectionTestUtils.setField(service, "modelMetadataRepository", repository);
        ReflectionTestUtils.setField(service, "modelSavePath", tempDir.toString());
        ReflectionTestUtils.setField(service, "minSamples", 30);
        ReflectionTestUtils.setField(service, "epochs", 40);
        ReflectionTestUtils.setField(service, "learningRate", 0.01d);
        ReflectionTestUtils.setField(service, "validationSplit", 0.2d);

        ModelMetadata trained = service.trainModel(userId);

        assertEquals(TrainingStatus.COMPLETED, trained.getTrainingStatus());
        assertNotNull(trained.getModelPath());
        assertTrue(new File(trained.getModelPath()).exists(), "Expected model file to be saved");

        Optional<NeuralNetworkModel> loaded = service.loadModel(userId);
        assertTrue(loaded.isPresent(), "Expected saved model to load");

        double[] probe = new double[NeuralNetworkModel.INPUT_SIZE];
        double[] prediction = loaded.get().predict(probe);
        assertEquals(NeuralNetworkModel.OUTPUT_SIZE, prediction.length);
    }

    @Test
    void trainModelFailsWhenInsufficientSamples() {
        String userId = "u-ai-too-small";
        FeatureExtractionService featureExtractionService = Mockito.mock(FeatureExtractionService.class);
        ModelMetadataRepository repository = Mockito.mock(ModelMetadataRepository.class);

        List<FeatureVector> tinyDataset = createDataset(userId, 3, 0.1);
        when(featureExtractionService.createDataSet(eq(userId))).thenReturn(tinyDataset);
        when(repository.save(any(ModelMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelTrainingService service = new ModelTrainingService();
        ReflectionTestUtils.setField(service, "featureExtractionService", featureExtractionService);
        ReflectionTestUtils.setField(service, "modelMetadataRepository", repository);
        ReflectionTestUtils.setField(service, "modelSavePath", tempDir.toString());
        ReflectionTestUtils.setField(service, "minSamples", 30);
        ReflectionTestUtils.setField(service, "epochs", 10);
        ReflectionTestUtils.setField(service, "learningRate", 0.01d);
        ReflectionTestUtils.setField(service, "validationSplit", 0.2d);

        ModelMetadata trained = service.trainModel(userId);

        assertEquals(TrainingStatus.FAILED, trained.getTrainingStatus());
    }

    private List<FeatureVector> createDataset(String userId, int rows, double transactionFrequency) {
        List<FeatureVector> data = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            double[] features = new double[NeuralNetworkModel.INPUT_SIZE];
            double[] labels = new double[NeuralNetworkModel.OUTPUT_SIZE];

            features[0] = 500 + (i * 25);
            features[1] = 100 + i;
            features[2] = 120 + i;
            features[19] = transactionFrequency;

            labels[0] = i % 2 == 0 ? 1.0 : 0.0;
            labels[3] = i % 3 == 0 ? 1.0 : 0.0;

            data.add(FeatureVector.builder()
                    .userId(userId)
                    .features(features)
                    .labels(labels)
                    .timestamp(LocalDateTime.now().minusMonths(rows - i))
                    .build());
        }
        return data;
    }
}

