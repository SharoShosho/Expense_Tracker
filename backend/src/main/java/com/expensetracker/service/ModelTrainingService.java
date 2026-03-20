package com.expensetracker.service;

import com.expensetracker.model.FeatureVector;
import com.expensetracker.model.ModelMetadata;
import com.expensetracker.model.TrainingStatus;
import com.expensetracker.nn.NeuralNetworkModel;
import com.expensetracker.repository.ModelMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Manages the full lifecycle of per-user prediction models:
 * training, persistence, loading, validation, and status tracking.
 */
@Service
public class ModelTrainingService {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainingService.class);

    private static final DateTimeFormatter VERSION_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @Value("${nn.model.save-path:/data/models}")
    private String modelSavePath;

    @Value("${nn.training.min-samples:30}")
    private int minSamples;

    @Value("${nn.training.epochs:100}")
    private int epochs;

    @Value("${nn.training.learning-rate:0.001}")
    private double learningRate;

    @Value("${nn.training.validation-split:0.2}")
    private double validationSplit;

    @Autowired
    private FeatureExtractionService featureExtractionService;

    @Autowired
    private ModelMetadataRepository modelMetadataRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Starts an async training job for the given user.
     * Returns immediately; status can be polled via {@link #getTrainingStatus(String)}.
     */
    @Async
    public void startTraining(String userId) {
        trainModel(userId);
    }

    /**
     * Main (synchronous) training logic.
     */
    public ModelMetadata trainModel(String userId) {
        String version = LocalDateTime.now().format(VERSION_FMT);
        ModelMetadata meta = ModelMetadata.builder()
                .userId(userId)
                .modelVersion(version)
                .trainingStatus(TrainingStatus.TRAINING)
                .trainingStartTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        meta = modelMetadataRepository.save(meta);

        try {
            // 1. Build dataset
            List<FeatureVector> dataset = featureExtractionService.createDataSet(userId);

            // Count raw transactions from features (proxy: non-zero samples)
            int totalExpenses = estimateTotalExpenses(dataset);
            if (totalExpenses < minSamples) {
                log.warn("User {} has only {} expense-equivalent samples — need at least {}",
                        userId, totalExpenses, minSamples);
                return failTraining(meta, "Insufficient data: need at least " + minSamples + " transactions");
            }

            if (dataset.size() < 3) {
                return failTraining(meta, "Need at least 3 months of expense history to train");
            }

            // 2. Normalise features
            double[][] rawFeatures = dataset.stream().map(FeatureVector::getFeatures).toArray(double[][]::new);
            double[][] normFeatures = featureExtractionService.normalizeFeatures(rawFeatures);

            // 3. Train/test split (80/20)
            int splitIdx = (int) Math.max(1, Math.floor(dataset.size() * (1.0 - validationSplit)));
            Slice trainSlice = toSlice(normFeatures, dataset, 0, splitIdx);
            Slice testSlice = toSlice(normFeatures, dataset, splitIdx, dataset.size());

            // 4. Build & train network
            NeuralNetworkModel model = new NeuralNetworkModel(learningRate);
            model.fit(trainSlice.features(), trainSlice.labels(), epochs, learningRate);

            // 5. Validate
            double accuracy = testSlice.size() > 0
                    ? evaluateAccuracy(model, testSlice.features(), testSlice.labels()) : 0.0;
            double loss = testSlice.size() > 0
                    ? model.binaryCrossEntropy(testSlice.features(), testSlice.labels()) : 0.0;

            // 6. Persist model
            String path = saveModel(userId, version, model);

            // 7. Update metadata
            meta.setTrainingStatus(TrainingStatus.COMPLETED);
            meta.setTrainingEndTime(LocalDateTime.now());
            meta.setAccuracy(accuracy);
            meta.setLossFunction(loss);
            meta.setTotalSamples(dataset.size());
            meta.setModelPath(path);
            meta.setUpdatedAt(LocalDateTime.now());
            meta = modelMetadataRepository.save(meta);

            log.info("Training completed for user {} - version={} accuracy={}",
                    userId, version, accuracy);

            // 8. Clean up old models (keep only the latest)
            deleteOldModels(userId, version);

            return meta;

        } catch (Exception e) {
            log.error("Training failed for user {}: {}", userId, e.getMessage(), e);
            return failTraining(meta, e.getMessage());
        }
    }

    /**
     * Saves a trained model to disk. Returns the file path.
     */
    public String saveModel(String userId, String version, NeuralNetworkModel model) throws IOException {
        File dir = new File(modelSavePath, userId);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create model directory: " + dir.getAbsolutePath());
        }
        File modelFile = new File(dir, version + ".zip");
        model.save(modelFile);
        return modelFile.getAbsolutePath();
    }

    /**
     * Loads a trained model from disk.
     *
     * @return the loaded network, or empty if no model exists
     */
    public Optional<NeuralNetworkModel> loadModel(String userId) {
        Optional<ModelMetadata> latestMeta =
                modelMetadataRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);

        if (latestMeta.isEmpty()
                || latestMeta.get().getTrainingStatus() != TrainingStatus.COMPLETED
                || latestMeta.get().getModelPath() == null) {
            return Optional.empty();
        }

        File modelFile = new File(latestMeta.get().getModelPath());
        if (!modelFile.exists()) {
            return Optional.empty();
        }

        try {
            return Optional.of(NeuralNetworkModel.load(modelFile));
        } catch (IOException e) {
            log.error("Failed to load model for user {}: {}", userId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /** Returns the latest training status for the user. */
    public TrainingStatus getTrainingStatus(String userId) {
        return modelMetadataRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(ModelMetadata::getTrainingStatus)
                .orElse(TrainingStatus.PENDING);
    }

    /** Returns the latest model metadata (accuracy, loss, version) for the user. */
    public Optional<ModelMetadata> getModelMetrics(String userId) {
        return modelMetadataRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Deletes all model metadata and files for the user. */
    public void deleteAllModels(String userId) {
        List<ModelMetadata> all = modelMetadataRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        for (ModelMetadata m : all) {
            if (m.getModelPath() != null) {
                new File(m.getModelPath()).delete();
            }
        }
        modelMetadataRepository.deleteByUserId(userId);
    }

    /**
     * Returns up to {@code limit} most recent training history records for the user.
     */
    public List<ModelMetadata> getTrainingHistory(String userId, int limit) {
        return modelMetadataRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Returns true if the user has a completed model that was trained within the last
     * {@code maxAgeDays} days.
     */
    public boolean hasRecentModel(String userId, int maxAgeDays) {
        return modelMetadataRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(m -> m.getTrainingStatus() == TrainingStatus.COMPLETED)
                .filter(m -> m.getTrainingEndTime() != null
                        && m.getTrainingEndTime().isAfter(LocalDateTime.now().minusDays(maxAgeDays)))
                .isPresent();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Slice toSlice(double[][] normFeatures, List<FeatureVector> vectors, int from, int to) {
        int rows = Math.max(0, to - from);
        double[][] featSlice = new double[rows][NeuralNetworkModel.INPUT_SIZE];
        double[][] labelSlice = new double[rows][NeuralNetworkModel.OUTPUT_SIZE];
        for (int i = 0; i < rows; i++) {
            featSlice[i] = normFeatures[from + i];
            labelSlice[i] = vectors.get(from + i).getLabels();
        }
        return new Slice(featSlice, labelSlice);
    }

    private double evaluateAccuracy(NeuralNetworkModel model, double[][] testFeatures, double[][] testLabels) {
        double[][] output = model.predict(testFeatures);
        long correct = 0;
        long total = 0;

        for (int r = 0; r < output.length; r++) {
            for (int c = 0; c < output[r].length; c++) {
                double pred = output[r][c] > 0.5 ? 1.0 : 0.0;
                double label = testLabels[r][c];
                if (pred == label) {
                    correct++;
                }
                total++;
            }
        }
        return total > 0 ? (double) correct / total : 0.0;
    }

    private ModelMetadata failTraining(ModelMetadata meta, String reason) {
        log.warn("Training marked FAILED for user {}: {}", meta.getUserId(), reason);
        meta.setTrainingStatus(TrainingStatus.FAILED);
        meta.setTrainingEndTime(LocalDateTime.now());
        meta.setUpdatedAt(LocalDateTime.now());
        return modelMetadataRepository.save(meta);
    }

    /** Deletes older model files / metadata, keeping only the most recent version. */
    private void deleteOldModels(String userId, String keepVersion) {
        List<ModelMetadata> all = modelMetadataRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        for (ModelMetadata m : all) {
            if (!keepVersion.equals(m.getModelVersion())) {
                if (m.getModelPath() != null) {
                    new File(m.getModelPath()).delete();
                }
                modelMetadataRepository.delete(m);
            }
        }
    }

    /**
     * Rough proxy for total transaction count across all sample months:
     * sum (feature[19] * days_in_month) ≈ total transactions.
     * Feature 19 is transactions/day; months have ~30 days on average.
     */
    private int estimateTotalExpenses(List<FeatureVector> dataset) {
        return dataset.stream()
                .mapToInt(fv -> (int) Math.round(fv.getFeatures()[19] * 30))
                .sum();
    }

    private record Slice(double[][] features, double[][] labels) {
        int size() {
            return features.length;
        }
    }
}
