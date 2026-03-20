package com.expensetracker.controller;

import com.expensetracker.dto.*;
import com.expensetracker.service.AuthenticatedUserService;
import com.expensetracker.service.SavingTipsEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private SavingTipsEngine savingTipsEngine;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @GetMapping("/tips/overview")
    public ResponseEntity<TipsOverviewDTO> getOverview(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.getOverview(userId));
    }

    @GetMapping("/tips/spending-pattern")
    public ResponseEntity<SpendingPatternDTO> getSpendingPattern(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.analyzeSpendingPattern(userId));
    }

    @GetMapping("/tips/behavioral")
    public ResponseEntity<BehavioralAnalysisDTO> getBehavioral(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.analyzeBehavior(userId));
    }

    @GetMapping("/tips/benchmarking")
    public ResponseEntity<BenchmarkingDTO> getBenchmarking(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.analyzeBenchmarking(userId));
    }

    @GetMapping("/tips/predictions")
    public ResponseEntity<PredictionDTO> getPredictions(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.analyzePredictions(userId));
    }

    @GetMapping("/tips/anomalies")
    public ResponseEntity<AnomalyDTO> getAnomalies(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.detectAnomalies(userId));
    }

    @GetMapping("/tips/category/{categoryName}")
    public ResponseEntity<CategoryAnalysisDTO> getCategoryDeepDive(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String categoryName) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.analyzeCategoryDeepDive(userId, categoryName));
    }

    @GetMapping("/tips/wellness-score")
    public ResponseEntity<WellnessScoreDTO> getWellnessScore(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.calculateWellnessScore(userId));
    }

    @GetMapping("/tips/history-trend")
    public ResponseEntity<HistoryTrendDTO> getHistoryTrend(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(savingTipsEngine.analyzeHistoryTrend(userId));
    }

    @PostMapping("/train")
    public ResponseEntity<Map<String, String>> triggerTraining(
            @AuthenticationPrincipal UserDetails userDetails) {
        // The rule-based engine does not require training
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Tips engine refreshed. Analysis is performed in real-time based on your latest data."
        ));
    }
}
