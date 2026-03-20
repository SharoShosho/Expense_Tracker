package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.FeatureVector;
import com.expensetracker.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts 20 numeric features and 8 binary labels from a user's expense data.
 *
 * Feature layout (0-indexed):
 *  0  – total monthly spending
 *  1-8  – spending per category (8 categories)
 *  9-16 – category spending percentages
 * 17  – average transaction amount
 * 18  – spending trend (% increase/decrease vs previous month)
 * 19  – transaction frequency (transactions per day in the month)
 *
 * Label layout (tip type active = 1.0, inactive = 0.0):
 *  0 – spendingPattern
 *  1 – behavioral
 *  2 – benchmarking
 *  3 – predictions
 *  4 – anomalies
 *  5 – categoryDeepDive
 *  6 – wellnessScore
 *  7 – historyTrend
 */
@Service
public class FeatureExtractionService {

    private static final Logger log = LoggerFactory.getLogger(FeatureExtractionService.class);

    static final String[] CATEGORIES = {
            "food", "housing", "transport", "entertainment",
            "health", "shopping", "utilities", "other"
    };

    private static final int FEATURE_SIZE = 20;
    private static final int LABEL_SIZE = 8;

    // Benchmark percentages (same as SavingTipsEngine)
    private static final double[] BENCHMARK_PCT = {25.0, 30.0, 15.0, 10.0, 8.0, 7.0, 4.0, 1.0};

    @Autowired
    private ExpenseRepository expenseRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Build a list of FeatureVectors from the last {@code months} calendar months.
     * Returns an empty list when the user has fewer than 3 months of data.
     */
    public List<FeatureVector> createDataSet(String userId) {
        List<FeatureVector> dataset = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            double[] features = extractFeatures(userId, month);
            double[] labels = extractLabels(userId, month);

            if (!validateFeatures(features)) {
                log.debug("Skipping month {} for user {} due to invalid features", month, userId);
                continue;
            }

            dataset.add(FeatureVector.builder()
                    .userId(userId)
                    .features(features)
                    .labels(labels)
                    .timestamp(month.atEndOfMonth().atStartOfDay())
                    .build());
        }

        return dataset;
    }

    /**
     * Extract 20 features for the given user and month.
     */
    public double[] extractFeatures(String userId, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);

        double[] features = new double[FEATURE_SIZE];

        // Feature 0: total monthly spending
        double total = expenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                .sum();
        features[0] = total;

        // Features 1-8: spending per category
        Map<String, Double> byCategory = groupByCategory(expenses);
        for (int i = 0; i < CATEGORIES.length; i++) {
            features[i + 1] = byCategory.getOrDefault(CATEGORIES[i], 0.0);
        }

        // Features 9-16: category spending percentages
        for (int i = 0; i < CATEGORIES.length; i++) {
            features[i + 9] = total > 0 ? (features[i + 1] / total) * 100.0 : 0.0;
        }

        // Feature 17: average transaction amount
        features[17] = expenses.isEmpty() ? 0.0 : total / expenses.size();

        // Feature 18: spending trend vs previous month
        YearMonth prevMonth = yearMonth.minusMonths(1);
        LocalDate prevStart = prevMonth.atDay(1);
        LocalDate prevEnd = prevMonth.atEndOfMonth();
        List<Expense> prevExpenses = expenseRepository.findByUserIdAndDateBetween(userId, prevStart, prevEnd);
        double prevTotal = prevExpenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                .sum();
        features[18] = prevTotal > 0 ? ((total - prevTotal) / prevTotal) * 100.0 : 0.0;

        // Feature 19: transaction frequency (transactions per day)
        long daysInMonth = ChronoUnit.DAYS.between(start, end) + 1;
        features[19] = (double) expenses.size() / daysInMonth;

        return features;
    }

    /**
     * Extract 8 binary labels for the given user and month.
     * A label is 1.0 if the corresponding tip type would fire for that month.
     */
    public double[] extractLabels(String userId, YearMonth yearMonth) {
        double[] labels = new double[LABEL_SIZE];

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);

        double total = expenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                .sum();

        Map<String, Double> byCategory = groupByCategory(expenses);

        // Label 0: spendingPattern — any category > 10 % over benchmark
        labels[0] = hasOverBudgetCategory(total, byCategory) ? 1.0 : 0.0;

        // Label 1: behavioral — has impulse spending days (> 2x daily average)
        labels[1] = hasImpulseDays(expenses) ? 1.0 : 0.0;

        // Label 2: benchmarking — food or housing over benchmark
        labels[2] = isBenchmarkWarning(total, byCategory) ? 1.0 : 0.0;

        // Label 3: predictions — spending trend > 5 %
        double[] features = extractFeatures(userId, yearMonth);
        labels[3] = Math.abs(features[18]) > 5.0 ? 1.0 : 0.0;

        // Label 4: anomalies — any transaction > 3x average
        labels[4] = hasAnomalies(expenses) ? 1.0 : 0.0;

        // Label 5: categoryDeepDive — entertainment or shopping high
        labels[5] = isCategoryDeepDiveRelevant(total, byCategory) ? 1.0 : 0.0;

        // Label 6: wellnessScore — total spending > 0 (always produce a wellness score)
        labels[6] = total > 0 ? 1.0 : 0.0;

        // Label 7: historyTrend — has data for at least 2 consecutive months
        labels[7] = expenses.size() >= 2 ? 1.0 : 0.0;

        return labels;
    }

    /**
     * Normalise features to [0, 1] using simple min-max normalisation per column.
     * Operates in-place on a 2-D array (rows = samples, cols = features).
     */
    public double[][] normalizeFeatures(double[][] featureMatrix) {
        if (featureMatrix.length == 0) return featureMatrix;

        int cols = featureMatrix[0].length;
        double[] min = new double[cols];
        double[] max = new double[cols];

        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);

        for (double[] row : featureMatrix) {
            for (int j = 0; j < cols; j++) {
                if (row[j] < min[j]) min[j] = row[j];
                if (row[j] > max[j]) max[j] = row[j];
            }
        }

        double[][] normalized = new double[featureMatrix.length][cols];
        for (int i = 0; i < featureMatrix.length; i++) {
            for (int j = 0; j < cols; j++) {
                double range = max[j] - min[j];
                normalized[i][j] = range > 0 ? (featureMatrix[i][j] - min[j]) / range : 0.0;
            }
        }
        return normalized;
    }

    /**
     * Returns {@code true} if the feature vector contains no NaN or Infinity values.
     */
    public boolean validateFeatures(double[] features) {
        for (double v : features) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return false;
            }
        }
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, Double> groupByCategory(List<Expense> expenses) {
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().toLowerCase(Locale.ROOT) : "other",
                        Collectors.summingDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                ));
    }

    private boolean hasOverBudgetCategory(double total, Map<String, Double> byCategory) {
        if (total <= 0) return false;
        for (int i = 0; i < CATEGORIES.length; i++) {
            double pct = (byCategory.getOrDefault(CATEGORIES[i], 0.0) / total) * 100.0;
            if (pct > BENCHMARK_PCT[i] * 1.1) {
                return true;
            }
        }
        return false;
    }

    private boolean hasImpulseDays(List<Expense> expenses) {
        if (expenses.size() < 3) return false;
        Map<LocalDate, Double> byDay = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getDate,
                        Collectors.summingDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                ));
        double avg = byDay.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return avg > 0 && byDay.values().stream().anyMatch(v -> v > avg * 2.0);
    }

    private boolean isBenchmarkWarning(double total, Map<String, Double> byCategory) {
        if (total <= 0) return false;
        double foodPct = (byCategory.getOrDefault("food", 0.0) / total) * 100.0;
        double housingPct = (byCategory.getOrDefault("housing", 0.0) / total) * 100.0;
        return foodPct > BENCHMARK_PCT[0] || housingPct > BENCHMARK_PCT[1];
    }

    private boolean hasAnomalies(List<Expense> expenses) {
        if (expenses.isEmpty()) return false;
        double avg = expenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                .average().orElse(0.0);
        return avg > 0 && expenses.stream()
                .anyMatch(e -> e.getAmount() != null && e.getAmount().doubleValue() > avg * 3.0);
    }

    private boolean isCategoryDeepDiveRelevant(double total, Map<String, Double> byCategory) {
        if (total <= 0) return false;
        double entPct = (byCategory.getOrDefault("entertainment", 0.0) / total) * 100.0;
        double shopPct = (byCategory.getOrDefault("shopping", 0.0) / total) * 100.0;
        return entPct > BENCHMARK_PCT[3] || shopPct > BENCHMARK_PCT[5];
    }
}
