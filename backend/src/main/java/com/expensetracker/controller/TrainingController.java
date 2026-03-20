package com.expensetracker.controller;

import com.expensetracker.model.ModelMetadata;
import com.expensetracker.model.TrainingStatus;
import com.expensetracker.service.AuthenticatedUserService;
import com.expensetracker.service.ModelTrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints for the Neural Network Training System.
 *
 * Base path: /api/ai/train
 */
@RestController
@RequestMapping("/api/ai/train")
public class TrainingController {

    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);

    private static final DateTimeFormatter VERSION_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @Autowired
    private ModelTrainingService modelTrainingService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/ai/train — start async training
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Map<String, String>> startTraining(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        String version = LocalDateTime.now().format(VERSION_FMT);

        log.info("Training requested for user {}", userId);
        modelTrainingService.startTraining(userId);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", TrainingStatus.TRAINING.name());
        response.put("modelVersion", version);
        return ResponseEntity.accepted().body(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/ai/train/status
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        TrainingStatus status = modelTrainingService.getTrainingStatus(userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status.name());
        response.put("progress", progressFor(status));
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/ai/train/metrics
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        Optional<ModelMetadata> meta = modelTrainingService.getModelMetrics(userId);

        if (meta.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No model trained yet"));
        }

        ModelMetadata m = meta.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accuracy", m.getAccuracy());
        response.put("loss", m.getLossFunction());
        response.put("modelVersion", m.getModelVersion());
        response.put("trainedAt", m.getTrainingEndTime());
        response.put("totalSamples", m.getTotalSamples());
        response.put("status", m.getTrainingStatus().name());
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/ai/train/history — last 10 training runs
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        List<Map<String, Object>> history = modelTrainingService.getTrainingHistory(userId, 10)
                .stream()
                .map(m -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("version", m.getModelVersion());
                    entry.put("accuracy", m.getAccuracy());
                    entry.put("loss", m.getLossFunction());
                    entry.put("trainedAt", m.getTrainingEndTime());
                    entry.put("status", m.getTrainingStatus().name());
                    return entry;
                })
                .toList();

        return ResponseEntity.ok(history);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE /api/ai/train/models — delete all models for the user
    // ──────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/models")
    public ResponseEntity<Map<String, Object>> deleteModels(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        modelTrainingService.deleteAllModels(userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "All models deleted");
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/ai/train/force-retrain — bypass scheduling
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/force-retrain")
    public ResponseEntity<Map<String, String>> forceRetrain(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        String version = LocalDateTime.now().format(VERSION_FMT);

        log.info("Force-retrain requested for user {}", userId);
        modelTrainingService.startTraining(userId);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", TrainingStatus.TRAINING.name());
        response.put("modelVersion", version);
        return ResponseEntity.accepted().body(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────────

    private int progressFor(TrainingStatus status) {
        return switch (status) {
            case PENDING   -> 0;
            case TRAINING  -> 50;
            case COMPLETED -> 100;
            case FAILED    -> 0;
        };
    }
}
