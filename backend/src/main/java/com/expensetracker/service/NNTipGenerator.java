package com.expensetracker.service;

import com.expensetracker.dto.SavingTipDTO;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates {@link SavingTipDTO} instances directly from neural-network output
 * confidences instead of relying solely on rule-based logic.
 *
 * <p>Confidence thresholds:
 * <ul>
 *   <li>0.0–0.4  → no tip generated</li>
 *   <li>0.4–0.6  → MEDIUM priority</li>
 *   <li>0.6–0.8  → HIGH priority</li>
 *   <li>0.8–1.0  → CRITICAL (mapped to HIGH with urgent wording)</li>
 * </ul>
 */
@Service
public class NNTipGenerator {

    private static final Logger log = LoggerFactory.getLogger(NNTipGenerator.class);

    // Neural network output indices (must match SavingTipsEngine constants)
    private static final int NN_IDX_SPENDING_PATTERN = 0;
    private static final int NN_IDX_BEHAVIORAL       = 1;
    private static final int NN_IDX_BENCHMARKING     = 2;
    private static final int NN_IDX_PREDICTIONS      = 3;
    private static final int NN_IDX_ANOMALIES        = 4;
    private static final int NN_IDX_CATEGORY         = 5;
    private static final int NN_IDX_WELLNESS         = 6;
    private static final int NN_IDX_HISTORY          = 7;

    // Minimum confidence before any tip is emitted
    private static final double MIN_CONFIDENCE = 0.4;

    // Industry-standard benchmark percentages (same as SavingTipsEngine)
    private static final Map<String, Double> BENCHMARK_PERCENTAGES = new LinkedHashMap<>();

    static {
        BENCHMARK_PERCENTAGES.put("Food", 25.0);
        BENCHMARK_PERCENTAGES.put("Housing", 30.0);
        BENCHMARK_PERCENTAGES.put("Transport", 15.0);
        BENCHMARK_PERCENTAGES.put("Entertainment", 10.0);
        BENCHMARK_PERCENTAGES.put("Health", 8.0);
        BENCHMARK_PERCENTAGES.put("Shopping", 7.0);
        BENCHMARK_PERCENTAGES.put("Utilities", 4.0);
        BENCHMARK_PERCENTAGES.put("Other", 1.0);
    }

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Iterates over all 8 NN output slots and, for each index with confidence
     * above {@value MIN_CONFIDENCE}, delegates to the appropriate generator.
     *
     * @param userId   the authenticated user
     * @param nnOutput the 8-element probability vector from the neural network
     * @return list of NN-generated tips (may be empty when nnOutput is null/short)
     */
    public List<SavingTipDTO> generateTipsFromNNOutput(String userId, double[] nnOutput) {
        List<SavingTipDTO> tips = new ArrayList<>();
        if (nnOutput == null) {
            return tips;
        }

        safeGenerate(tips, NN_IDX_SPENDING_PATTERN, nnOutput, () -> generateSpendingPatternTip(userId, nnOutput[NN_IDX_SPENDING_PATTERN]));
        safeGenerate(tips, NN_IDX_BEHAVIORAL,       nnOutput, () -> generateBehavioralTip(userId, nnOutput[NN_IDX_BEHAVIORAL]));
        safeGenerate(tips, NN_IDX_BENCHMARKING,     nnOutput, () -> generateBenchmarkingTip(userId, nnOutput[NN_IDX_BENCHMARKING]));
        safeGenerate(tips, NN_IDX_PREDICTIONS,      nnOutput, () -> generatePredictionTip(userId, nnOutput[NN_IDX_PREDICTIONS]));
        safeGenerate(tips, NN_IDX_ANOMALIES,        nnOutput, () -> generateAnomalyTip(userId, nnOutput[NN_IDX_ANOMALIES]));
        safeGenerate(tips, NN_IDX_CATEGORY,         nnOutput, () -> generateCategoryTip(userId, nnOutput[NN_IDX_CATEGORY]));
        safeGenerate(tips, NN_IDX_WELLNESS,         nnOutput, () -> generateWellnessTip(userId, nnOutput[NN_IDX_WELLNESS]));
        safeGenerate(tips, NN_IDX_HISTORY,          nnOutput, () -> generateHistoryTip(userId, nnOutput[NN_IDX_HISTORY]));

        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Per-type generators
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generates a Spending Pattern tip based on the user's budget utilisation.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generateSpendingPatternTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Expense> expenses = getMonthlyExpenses(userId, YearMonth.now());
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);

        double totalSpent  = sumExpenses(expenses);
        double totalBudget = sumBudgets(budgets);

        // Find highest-overspend category
        Map<String, Double> spentByCategory = groupByCategoryNormalized(expenses);
        String focusCategory = budgets.stream()
                .filter(b -> b.getAmount() != null && b.getAmount().doubleValue() > 0)
                .max(Comparator.comparingDouble(b -> {
                    double spent = spentByCategory.getOrDefault(b.getCategory().toLowerCase(Locale.ROOT), 0.0);
                    return spent - b.getAmount().doubleValue();
                }))
                .map(Budget::getCategory)
                .orElse(null);

        String priority = confidenceToPriority(confidence);
        double savingsPotential = totalBudget > 0 && totalSpent > totalBudget
                ? round((totalSpent - totalBudget) / totalSpent * 100) : 0;

        String message;
        List<String> actions;

        if (confidence > 0.7 && focusCategory != null) {
            message = String.format(
                    "Model predicts high impact: focus on %s. You could save up to %.1f%% of current spending.",
                    focusCategory, savingsPotential);
            actions = List.of(
                    "Review all " + focusCategory + " transactions this month",
                    "Set a stricter budget for " + focusCategory,
                    String.format("AI model confidence: %.0f%%", confidence * 100));
        } else {
            message = String.format(
                    "Your current spending is %.1f%% of your budget. AI detects an opportunity to optimise.",
                    totalBudget > 0 ? round(totalSpent / totalBudget * 100) : 0);
            actions = List.of(
                    "Review your top 3 spending categories",
                    String.format("AI model confidence: %.0f%%", confidence * 100));
        }

        return SavingTipDTO.builder()
                .title("AI: Spending Pattern Opportunity")
                .message(message)
                .priority(priority)
                .potentialSavings(savingsPotential)
                .actionItems(actions)
                .build();
    }

    /**
     * Generates a Behavioural tip based on weekday vs weekend spending and
     * impulse-purchase patterns.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generateBehavioralTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Expense> expenses = getMonthlyExpenses(userId, YearMonth.now());
        if (expenses.isEmpty()) return null;

        Map<DayOfWeek, Double> byDayOfWeek = expenses.stream()
                .filter(e -> e.getDate() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getDate().getDayOfWeek(),
                        Collectors.summingDouble(e -> toDouble(e.getAmount()))));

        DayOfWeek peakDay = byDayOfWeek.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DayOfWeek.MONDAY);

        double weekdayTotal  = byDayOfWeek.entrySet().stream()
                .filter(e -> e.getKey() != DayOfWeek.SATURDAY && e.getKey() != DayOfWeek.SUNDAY)
                .mapToDouble(Map.Entry::getValue).sum();
        double weekendTotal  = byDayOfWeek.entrySet().stream()
                .filter(e -> e.getKey() == DayOfWeek.SATURDAY || e.getKey() == DayOfWeek.SUNDAY)
                .mapToDouble(Map.Entry::getValue).sum();

        boolean overspendWeekend = weekendTotal > weekdayTotal * 0.4; // weekend should be ~2/7 of week

        String peakDayName = peakDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String priority = confidenceToPriority(confidence);

        String message = String.format(
                "Model detects you overspend on %s (confidence: %.0f%%). %s",
                peakDayName, confidence * 100,
                overspendWeekend ? "Weekend spending is disproportionately high." : "");

        return SavingTipDTO.builder()
                .title("AI: Behavioural Pattern Alert")
                .message(message)
                .priority(priority)
                .potentialSavings(0)
                .actionItems(List.of(
                        "Plan activities for " + peakDayName + " in advance to curb impulse spending",
                        "Set a daily spending limit for weekends",
                        String.format("AI model confidence: %.0f%%", confidence * 100)))
                .build();
    }

    /**
     * Generates a Benchmarking tip highlighting the category most above the
     * industry average.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generateBenchmarkingTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Expense> expenses = getMonthlyExpenses(userId, YearMonth.now());
        double totalSpent = sumExpenses(expenses);
        if (totalSpent == 0) return null;

        Map<String, Double> spentByCategory = groupByCategoryNormalized(expenses);

        // Find the category most above benchmark
        String worstCategory = null;
        double worstDiff = 0;
        for (Map.Entry<String, Double> entry : BENCHMARK_PERCENTAGES.entrySet()) {
            double userPct = spentByCategory.getOrDefault(entry.getKey().toLowerCase(Locale.ROOT), 0.0)
                    / totalSpent * 100;
            double diff = userPct - entry.getValue();
            if (diff > worstDiff) {
                worstDiff = diff;
                worstCategory = entry.getKey();
            }
        }

        if (worstCategory == null) return null;

        String priority = confidenceToPriority(confidence);
        return SavingTipDTO.builder()
                .title("AI: Above-Average Spending Detected")
                .message(String.format(
                        "AI finds you spend %.1f%% more on %s than peers. Consider optimising.",
                        worstDiff, worstCategory))
                .priority(priority)
                .potentialSavings(round(worstDiff / 100 * totalSpent))
                .actionItems(List.of(
                        "Compare your " + worstCategory + " expenses with last month",
                        "Look for cheaper alternatives in the " + worstCategory + " category",
                        String.format("AI model confidence: %.0f%%", confidence * 100)))
                .build();
    }

    /**
     * Generates a Prediction tip based on the detected spending trend.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generatePredictionTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Double> monthlyTotals = getMonthlyTotals(userId, 6);
        if (monthlyTotals.stream().allMatch(v -> v == 0)) return null;

        double trend = calculateTrend(monthlyTotals);
        String trendLabel = trend > 10 ? "INCREASING" : (trend < -5 ? "DECREASING" : "STABLE");
        String severity   = trend > 15 ? "HIGH" : (trend > 5 ? "MEDIUM" : "LOW");

        String priority = confidenceToPriority(confidence);
        return SavingTipDTO.builder()
                .title("AI: Spending Trend Forecast")
                .message(String.format(
                        "Model predicts %s trend - %s alert. Monthly change: %+.1f%%.",
                        trendLabel, severity, trend))
                .priority(priority)
                .potentialSavings(0)
                .actionItems(List.of(
                        "INCREASING".equals(trendLabel) ? "Identify categories driving the increase and cut back" :
                        "DECREASING".equals(trendLabel) ? "Channel savings into an emergency fund or investment" :
                        "Keep monitoring your spending each month",
                        String.format("AI model confidence: %.0f%%", confidence * 100)))
                .build();
    }

    /**
     * Generates an Anomaly tip highlighting a category with unusual transactions.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generateAnomalyTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Expense> allExpenses    = expenseRepository.findByUserId(userId);
        List<Expense> recentExpenses = getMonthlyExpenses(userId, YearMonth.now());
        if (recentExpenses.isEmpty()) return null;

        Map<String, Double> avgByCategory = allExpenses.stream()
                .filter(e -> e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getCategory()).orElse("Other").toLowerCase(Locale.ROOT),
                        Collectors.averagingDouble(e -> toDouble(e.getAmount()))));

        double overallAvg = allExpenses.stream()
                .mapToDouble(e -> toDouble(e.getAmount())).average().orElse(0);

        String anomalyCategory = recentExpenses.stream()
                .filter(e -> {
                    String key = Optional.ofNullable(e.getCategory()).orElse("Other").toLowerCase(Locale.ROOT);
                    double avg = avgByCategory.getOrDefault(key, overallAvg);
                    return avg > 0 && toDouble(e.getAmount()) > avg * 2.0;
                })
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                        Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (anomalyCategory == null) return null;

        String priority = confidenceToPriority(confidence);
        return SavingTipDTO.builder()
                .title("AI: Unusual Activity Detected")
                .message(String.format(
                        "Unusual activity detected in %s — transactions above 2× your historical average.",
                        anomalyCategory))
                .priority(priority)
                .potentialSavings(0)
                .actionItems(List.of(
                        "Review your recent " + anomalyCategory + " transactions",
                        "Check if any purchase can be returned or refunded",
                        String.format("AI model confidence: %.0f%%", confidence * 100)))
                .build();
    }

    /**
     * Generates a Category tip focused on the highest-impact category.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generateCategoryTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Expense> expenses = getMonthlyExpenses(userId, YearMonth.now());
        if (expenses.isEmpty()) return null;

        Map<String, Double> spentByCategory = groupByCategory(expenses);
        String topCategory = spentByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (topCategory == null) return null;

        double topAmount = spentByCategory.get(topCategory);
        double totalSpent = sumExpenses(expenses);
        double pct = totalSpent > 0 ? round(topAmount / totalSpent * 100) : 0;

        String priority = confidenceToPriority(confidence);
        return SavingTipDTO.builder()
                .title("AI: Priority Category Flagged")
                .message(String.format(
                        "Model flags %s as your priority area (%.1f%% of total spend). Optimise here for maximum impact.",
                        topCategory, pct))
                .priority(priority)
                .potentialSavings(round(topAmount * 0.1))
                .actionItems(List.of(
                        "Review all " + topCategory + " transactions for unnecessary spending",
                        "Set or tighten a monthly budget limit for " + topCategory,
                        "Find cheaper alternatives for your top " + topCategory + " purchases",
                        String.format("AI model confidence: %.0f%%", confidence * 100)))
                .build();
    }

    /**
     * Generates a Wellness tip pointing to the lowest-scoring wellness dimension.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generateWellnessTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Budget> budgets  = budgetRepository.findByUserIdOrderByCategoryAsc(userId);
        List<Expense> expenses = getMonthlyExpenses(userId, YearMonth.now());

        double totalSpent  = sumExpenses(expenses);
        double totalBudget = sumBudgets(budgets);

        // Determine the weakest wellness dimension
        String dimension;
        String actionableStep;

        if (budgets.isEmpty()) {
            dimension = "Financial Awareness";
            actionableStep = "Create budgets for all major spending categories";
        } else if (totalBudget > 0 && totalSpent > totalBudget) {
            dimension = "Spending Discipline";
            actionableStep = String.format("Reduce spending by at least %.2f to stay within budget",
                    totalSpent - totalBudget);
        } else if (totalBudget > 0 && (totalBudget - totalSpent) / totalBudget < 0.1) {
            dimension = "Saving Rate";
            actionableStep = "Try to keep at least 10% of your monthly budget unspent";
        } else {
            dimension = "Budget Adherence";
            actionableStep = "Review categories where you frequently exceed limits";
        }

        String priority = confidenceToPriority(confidence);
        return SavingTipDTO.builder()
                .title("AI: Wellness Improvement Opportunity")
                .message(String.format(
                        "Improve %s to boost your financial wellness score.", dimension))
                .priority(priority)
                .potentialSavings(0)
                .actionItems(List.of(
                        actionableStep,
                        "Track your wellness score monthly to monitor progress",
                        String.format("AI model confidence: %.0f%%", confidence * 100)))
                .build();
    }

    /**
     * Generates a History tip based on the 6-month spending trend.
     *
     * @param userId     the authenticated user
     * @param confidence NN confidence for this tip type
     * @return a tip, or {@code null} when confidence is too low
     */
    public SavingTipDTO generateHistoryTip(String userId, double confidence) {
        if (confidence < MIN_CONFIDENCE) return null;

        List<Double> monthlyTotals = getMonthlyTotals(userId, 6);
        if (monthlyTotals.stream().allMatch(v -> v == 0)) return null;

        double trend     = calculateTrend(monthlyTotals);
        String direction = trend > 5 ? "upward" : (trend < -5 ? "downward" : "stable");
        boolean urgent   = Math.abs(trend) > 15;

        String priority = confidenceToPriority(confidence);
        String title    = urgent ? "AI: Critical History Alert" : "AI: 6-Month Trend Detected";
        String message  = String.format(
                "Model detected %s trend over 6 months (%.1f%%/month) — %s.",
                direction, trend, urgent ? "act now" : "monitor closely");

        List<String> actions = new ArrayList<>();
        if ("upward".equals(direction)) {
            actions.add("Audit the last 6 months for new recurring charges");
            actions.add("Target the fastest-growing expense category first");
        } else if ("downward".equals(direction)) {
            actions.add("Redirect your savings into investments or an emergency fund");
            actions.add("Set a new, lower monthly spending target to lock in your progress");
        } else {
            actions.add("Introduce one cost-saving measure this month to break the plateau");
        }
        actions.add(String.format("AI model confidence: %.0f%%", confidence * 100));

        return SavingTipDTO.builder()
                .title(title)
                .message(message)
                .priority(priority)
                .potentialSavings(0)
                .actionItems(Collections.unmodifiableList(actions))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Maps a confidence value to a tip priority string.
     *
     * <pre>
     * [0.0, 0.4)  → (caller should not reach this; guarded by MIN_CONFIDENCE)
     * [0.4, 0.6)  → "MEDIUM"
     * [0.6, 0.8)  → "HIGH"
     * [0.8, 1.0]  → "HIGH" (CRITICAL – indicated in the message wording)
     * </pre>
     */
    String confidenceToPriority(double confidence) {
        if (confidence >= 0.6) return "HIGH";
        if (confidence >= 0.4) return "MEDIUM";
        return "LOW";
    }

    private void safeGenerate(List<SavingTipDTO> collector, int idx, double[] nnOutput,
                               java.util.function.Supplier<SavingTipDTO> generator) {
        if (idx >= nnOutput.length) return;
        if (nnOutput[idx] < MIN_CONFIDENCE) return;
        try {
            SavingTipDTO tip = generator.get();
            if (tip != null) {
                collector.add(tip);
            }
        } catch (Exception e) {
            log.warn("NNTipGenerator failed for index {}: {}", idx, e.getMessage());
        }
    }

    private List<Expense> getMonthlyExpenses(String userId, YearMonth month) {
        return expenseRepository.findByUserIdAndDateBetween(
                userId, month.atDay(1), month.atEndOfMonth());
    }

    private List<Double> getMonthlyTotals(String userId, int months) {
        List<Double> totals = new ArrayList<>();
        YearMonth startMonth = YearMonth.now().minusMonths(months - 1);
        for (int i = 0; i < months; i++) {
            totals.add(sumExpenses(getMonthlyExpenses(userId, startMonth.plusMonths(i))));
        }
        return totals;
    }

    private double calculateTrend(List<Double> values) {
        List<Double> nonZero = values.stream().filter(v -> v > 0).collect(Collectors.toList());
        if (nonZero.size() < 2) return 0;
        double first   = nonZero.get(0);
        double last    = nonZero.get(nonZero.size() - 1);
        int    periods = nonZero.size() - 1;
        if (periods == 0 || first == 0) return 0;
        return ((last - first) / first / periods) * 100;
    }

    private double sumExpenses(List<Expense> expenses) {
        return expenses.stream()
                .filter(e -> e.getAmount() != null)
                .mapToDouble(e -> toDouble(e.getAmount()))
                .sum();
    }

    private double sumBudgets(List<Budget> budgets) {
        return budgets.stream()
                .filter(b -> b.getAmount() != null)
                .mapToDouble(b -> b.getAmount().doubleValue())
                .sum();
    }

    private Map<String, Double> groupByCategory(List<Expense> expenses) {
        return expenses.stream()
                .filter(e -> e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                        Collectors.summingDouble(e -> toDouble(e.getAmount()))));
    }

    private Map<String, Double> groupByCategoryNormalized(List<Expense> expenses) {
        return expenses.stream()
                .filter(e -> e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getCategory()).orElse("Other").toLowerCase(Locale.ROOT),
                        Collectors.summingDouble(e -> toDouble(e.getAmount()))));
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
