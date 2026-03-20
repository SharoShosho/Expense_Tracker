package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
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
import java.util.stream.IntStream;

@Service
public class SavingTipsEngine {

    private static final Logger log = LoggerFactory.getLogger(SavingTipsEngine.class);

    // Neural network output index → tip type mapping
    private static final int NN_IDX_SPENDING_PATTERN = 0;
    private static final int NN_IDX_BEHAVIORAL       = 1;
    private static final int NN_IDX_BENCHMARKING     = 2;
    private static final int NN_IDX_PREDICTIONS      = 3;
    private static final int NN_IDX_ANOMALIES        = 4;
    private static final int NN_IDX_CATEGORY         = 5;
    private static final int NN_IDX_WELLNESS         = 6;
    private static final int NN_IDX_HISTORY          = 7;

    // Staleness threshold: re-use model if it is less than 7 days old
    private static final int NN_MAX_AGE_DAYS = 7;

    // Benchmark percentages of total spending (industry-standard budgeting guidelines)
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

    @Autowired(required = false)
    private ModelTrainingService modelTrainingService;

    @Autowired(required = false)
    private FeatureExtractionService featureExtractionService;

    // ──────────────────────────────────────────────────────────────────────────
    // Type 1: Spending Pattern Analysis
    // ──────────────────────────────────────────────────────────────────────────

    public SpendingPatternDTO analyzeSpendingPattern(String userId) {
        YearMonth currentMonth = YearMonth.now();
        List<Expense> monthlyExpenses = getMonthlyExpenses(userId, currentMonth);
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);

        double totalSpent = sumExpenses(monthlyExpenses);
        double totalBudget = sumBudgets(budgets);

        Map<String, Double> spentByCategory = groupByCategory(monthlyExpenses);
        Map<String, Double> budgetByCategory = budgets.stream()
                .collect(Collectors.toMap(
                        b -> b.getCategory().toLowerCase(Locale.ROOT),
                        b -> b.getAmount() != null ? b.getAmount().doubleValue() : 0.0
                ));

        List<CategorySpendingDTO> categories = buildCategorySpendingList(
                spentByCategory, budgetByCategory, totalSpent);

        List<String> overBudget = categories.stream()
                .filter(c -> "EXCEEDED".equals(c.getStatus()))
                .map(CategorySpendingDTO::getCategory)
                .collect(Collectors.toList());

        double potentialSavings = categories.stream()
                .filter(c -> "EXCEEDED".equals(c.getStatus()))
                .mapToDouble(CategorySpendingDTO::getOverBudgetAmount)
                .sum();

        List<SavingTipDTO> tips = buildSpendingPatternTips(categories, totalSpent, totalBudget);
        double[] nnOutput = getNNPredictions(userId);
        tips = enhanceTipsWithNNPredictions(tips, nnOutput, NN_IDX_SPENDING_PATTERN);

        double budgetUsage = totalBudget > 0 ? (totalSpent / totalBudget) * 100 : 0;

        return SpendingPatternDTO.builder()
                .totalMonthlySpending(totalSpent)
                .totalBudget(totalBudget)
                .budgetUsagePercent(round(budgetUsage))
                .categories(categories)
                .overBudgetCategories(overBudget)
                .totalPotentialSavings(round(potentialSavings))
                .tips(tips)
                .build();
    }

    private List<SavingTipDTO> buildSpendingPatternTips(
            List<CategorySpendingDTO> categories, double totalSpent, double totalBudget) {
        List<SavingTipDTO> tips = new ArrayList<>();

        for (CategorySpendingDTO cat : categories) {
            if ("EXCEEDED".equals(cat.getStatus())) {
                tips.add(SavingTipDTO.builder()
                        .title("Over Budget: " + cat.getCategory())
                        .message(String.format(
                                "You have exceeded your %s budget by %.2f. " +
                                "You spent %.2f out of a %.2f budget (%.1f%%).",
                                cat.getCategory(),
                                cat.getOverBudgetAmount(),
                                cat.getAmount().doubleValue(),
                                cat.getBudget() != null ? cat.getBudget().doubleValue() : 0,
                                cat.getPercentage()))
                        .priority("HIGH")
                        .potentialSavings(round(cat.getOverBudgetAmount()))
                        .actionItems(List.of(
                                "Review all " + cat.getCategory() + " transactions this month",
                                "Identify which purchases were non-essential",
                                "Set a stricter daily limit for " + cat.getCategory(),
                                "Track every " + cat.getCategory() + " purchase before making it"
                        ))
                        .build());
            } else if ("NEAR_LIMIT".equals(cat.getStatus())) {
                tips.add(SavingTipDTO.builder()
                        .title("Approaching Limit: " + cat.getCategory())
                        .message(String.format(
                                "You have used %.1f%% of your %s budget with more days remaining in the month.",
                                cat.getPercentage(), cat.getCategory()))
                        .priority("MEDIUM")
                        .potentialSavings(0)
                        .actionItems(List.of(
                                "Slow down spending in " + cat.getCategory() + " this month",
                                "Plan your remaining " + cat.getCategory() + " expenses in advance",
                                "Look for cheaper alternatives"
                        ))
                        .build());
            }
        }

        if (totalBudget > 0 && totalSpent > totalBudget * 0.9 && tips.isEmpty()) {
            tips.add(SavingTipDTO.builder()
                    .title("Total Budget Nearly Exhausted")
                    .message(String.format(
                            "You have spent %.2f of your total %.2f budget (%.1f%%). Be careful for the rest of the month.",
                            totalSpent, totalBudget, (totalSpent / totalBudget) * 100))
                    .priority("MEDIUM")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Avoid non-essential purchases for the remainder of the month",
                            "Review upcoming expenses and defer what you can"
                    ))
                    .build());
        }

        if (tips.isEmpty()) {
            tips.add(SavingTipDTO.builder()
                    .title("Great Budget Discipline!")
                    .message("You are well within your budget this month. Keep up the excellent financial habits.")
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Consider moving surplus funds to savings",
                            "Review your budget limits and adjust if they are too conservative"
                    ))
                    .build());
        }

        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type 2: Behavioral Pattern Recognition
    // ──────────────────────────────────────────────────────────────────────────

    public BehavioralAnalysisDTO analyzeBehavior(String userId) {
        YearMonth currentMonth = YearMonth.now();
        List<Expense> expenses = getMonthlyExpenses(userId, currentMonth);

        if (expenses.isEmpty()) {
            return BehavioralAnalysisDTO.builder()
                    .dailyTransactionCount(0)
                    .peakSpendingDay("N/A")
                    .peakSpendingHour("N/A")
                    .weekdayAvgSpending(0)
                    .weekendAvgSpending(0)
                    .impulseTransactionCount(0)
                    .tips(List.of(noDataTip()))
                    .build();
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        double avgDaily = sumExpenses(expenses) / daysInMonth;

        // Peak spending day of week
        Map<DayOfWeek, Double> byDayOfWeek = expenses.stream()
                .filter(e -> e.getDate() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getDate().getDayOfWeek(),
                        Collectors.summingDouble(e -> toDouble(e.getAmount()))
                ));
        DayOfWeek peakDay = byDayOfWeek.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DayOfWeek.MONDAY);

        // Weekday vs weekend spending
        double weekdayTotal = byDayOfWeek.entrySet().stream()
                .filter(e -> e.getKey() != DayOfWeek.SATURDAY && e.getKey() != DayOfWeek.SUNDAY)
                .mapToDouble(Map.Entry::getValue).sum();
        double weekendTotal = byDayOfWeek.entrySet().stream()
                .filter(e -> e.getKey() == DayOfWeek.SATURDAY || e.getKey() == DayOfWeek.SUNDAY)
                .mapToDouble(Map.Entry::getValue).sum();

        long weekdays = byDayOfWeek.keySet().stream()
                .filter(d -> d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY).count();
        long weekendDays = byDayOfWeek.keySet().stream()
                .filter(d -> d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY).count();

        double weekdayAvg = weekdays > 0 ? weekdayTotal / weekdays : 0;
        double weekendAvg = weekendDays > 0 ? weekendTotal / weekendDays : 0;

        // Impulse detection: same-day multiple transactions above average
        Map<LocalDate, Long> txPerDay = expenses.stream()
                .filter(e -> e.getDate() != null)
                .collect(Collectors.groupingBy(Expense::getDate, Collectors.counting()));
        int impulseDays = (int) txPerDay.values().stream().filter(count -> count > 3).count();

        // Avg transaction amount
        double avgTx = expenses.stream()
                .mapToDouble(e -> toDouble(e.getAmount())).average().orElse(0);
        int impulseCount = (int) expenses.stream()
                .filter(e -> toDouble(e.getAmount()) > avgTx * 1.5).count();

        List<SavingTipDTO> tips = buildBehavioralTips(weekdayAvg, weekendAvg, impulseDays, impulseCount, peakDay);
        double[] nnOutput = getNNPredictions(userId);
        tips = enhanceTipsWithNNPredictions(tips, nnOutput, NN_IDX_BEHAVIORAL);

        return BehavioralAnalysisDTO.builder()
                .dailyTransactionCount((int) (expenses.size() / Math.max(daysInMonth, 1)))
                .peakSpendingDay(peakDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .peakSpendingHour("N/A")
                .weekdayAvgSpending(round(weekdayAvg))
                .weekendAvgSpending(round(weekendAvg))
                .impulseTransactionCount(impulseCount)
                .tips(tips)
                .build();
    }

    private List<SavingTipDTO> buildBehavioralTips(double weekdayAvg, double weekendAvg,
                                                    int impulseDays, int impulseCount,
                                                    DayOfWeek peakDay) {
        List<SavingTipDTO> tips = new ArrayList<>();

        if (weekendAvg > weekdayAvg * 1.5) {
            tips.add(SavingTipDTO.builder()
                    .title("High Weekend Spending")
                    .message(String.format(
                            "Your weekend spending (avg %.2f/day) is significantly higher than weekday spending (avg %.2f/day).",
                            weekendAvg, weekdayAvg))
                    .priority("MEDIUM")
                    .potentialSavings(round((weekendAvg - weekdayAvg) * 8))
                    .actionItems(List.of(
                            "Plan free or low-cost weekend activities",
                            "Set a weekend spending limit",
                            "Cook at home on weekends instead of dining out"
                    ))
                    .build());
        }

        if (impulseCount > 3) {
            tips.add(SavingTipDTO.builder()
                    .title("Impulse Purchases Detected")
                    .message(String.format(
                            "You have %d transactions that appear to be impulse purchases (50%% above your average transaction amount).",
                            impulseCount))
                    .priority("HIGH")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Apply a 24-hour cool-off period before large purchases",
                            "Ask yourself: Is this a need or a want?",
                            "Use a shopping list and stick to it",
                            "Unsubscribe from promotional emails"
                    ))
                    .build());
        }

        if (impulseDays > 2) {
            tips.add(SavingTipDTO.builder()
                    .title("Frequent Transaction Days")
                    .message(String.format(
                            "You had %d days this month with more than 3 transactions per day, suggesting frequent impulse spending.",
                            impulseDays))
                    .priority("MEDIUM")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Limit shopping trips to once or twice per week",
                            "Consolidate small purchases into fewer trips",
                            "Try the \"one transaction per day\" rule"
                    ))
                    .build());
        }

        String peakDayName = peakDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        tips.add(SavingTipDTO.builder()
                .title("Peak Spending Day: " + peakDayName)
                .message(String.format(
                        "%s is your highest spending day of the week. Being aware of this pattern can help you plan ahead.",
                        peakDayName))
                .priority("LOW")
                .potentialSavings(0)
                .actionItems(List.of(
                        "Plan your " + peakDayName + " activities in advance",
                        "Set a specific spending limit for " + peakDayName,
                        "Avoid browsing shops or online stores on " + peakDayName
                ))
                .build());

        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type 3: Comparative Benchmarking
    // ──────────────────────────────────────────────────────────────────────────

    public BenchmarkingDTO analyzeBenchmarking(String userId) {
        YearMonth currentMonth = YearMonth.now();
        List<Expense> expenses = getMonthlyExpenses(userId, currentMonth);

        double totalSpent = sumExpenses(expenses);
        Map<String, Double> spentByCategory = groupByCategoryNormalized(expenses);

        // Convert to percentage of total
        Map<String, Double> userPercentages = new LinkedHashMap<>();
        Map<String, Double> avgPercentages = new LinkedHashMap<>();

        for (String category : BENCHMARK_PERCENTAGES.keySet()) {
            double userSpent = spentByCategory.getOrDefault(category.toLowerCase(Locale.ROOT), 0.0);
            double userPct = totalSpent > 0 ? (userSpent / totalSpent) * 100 : 0;
            userPercentages.put(category, round(userPct));
            avgPercentages.put(category, BENCHMARK_PERCENTAGES.get(category));
        }

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<SavingTipDTO> tips = new ArrayList<>();

        for (String category : BENCHMARK_PERCENTAGES.keySet()) {
            double userPct = userPercentages.get(category);
            double avgPct = avgPercentages.get(category);
            double diff = userPct - avgPct;

            if (userPct > 0 && diff > 5) {
                weaknesses.add(String.format("%s: %.1f%% vs benchmark %.1f%% (+%.1f%%)",
                        category, userPct, avgPct, diff));
                tips.add(SavingTipDTO.builder()
                        .title("High " + category + " Spending vs Benchmark")
                        .message(String.format(
                                "You spend %.1f%% of your budget on %s, while the typical person spends %.1f%%. " +
                                "This is %.1f%% higher than average.",
                                userPct, category, avgPct, diff))
                        .priority(diff > 15 ? "HIGH" : "MEDIUM")
                        .potentialSavings(totalSpent > 0 ? round((diff / 100) * totalSpent) : 0)
                        .actionItems(List.of(
                                "Review your " + category + " expenses for the past 3 months",
                                "Find specific recurring costs to eliminate or reduce",
                                "Research cheaper alternatives for your top " + category + " expenses"
                        ))
                        .build());
            } else if (userPct > 0 && diff < -5) {
                strengths.add(String.format("%s: %.1f%% vs benchmark %.1f%% (%.1f%% under)",
                        category, userPct, avgPct, Math.abs(diff)));
            }
        }

        if (tips.isEmpty() && totalSpent > 0) {
            tips.add(SavingTipDTO.builder()
                    .title("Well-Balanced Spending")
                    .message("Your spending across categories is well-aligned with typical benchmarks. Well done!")
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of("Continue monitoring your spending patterns monthly"))
                    .build());
        }

        return BenchmarkingDTO.builder()
                .userSpendingByCategory(userPercentages)
                .avgSpendingByCategory(avgPercentages)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .tips(enhanceTipsWithNNPredictions(tips, getNNPredictions(userId), NN_IDX_BENCHMARKING))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type 4: Predictive Analysis & Forecasting
    // ──────────────────────────────────────────────────────────────────────────

    public PredictionDTO analyzePredictions(String userId) {
        List<Double> monthlyTotals = getMonthlyTotals(userId, 6);

        double currentMonthSpending = monthlyTotals.isEmpty() ? 0 : monthlyTotals.get(monthlyTotals.size() - 1);

        double trendPercent = calculateTrend(monthlyTotals);

        List<MonthPredictionDTO> predictions = new ArrayList<>();
        double base = currentMonthSpending > 0 ? currentMonthSpending
                : (monthlyTotals.isEmpty() ? 0 : monthlyTotals.stream().mapToDouble(d -> d).average().orElse(0));
        double multiplier = 1 + (trendPercent / 100);

        for (int i = 1; i <= 3; i++) {
            YearMonth futureMonth = YearMonth.now().plusMonths(i);
            double predicted = base * Math.pow(multiplier, i);
            String risk = predicted > base * 1.3 ? "HIGH" : predicted > base * 1.1 ? "MEDIUM" : "LOW";

            String recommendation;
            if ("HIGH".equals(risk)) {
                recommendation = String.format(
                        "Alert: Spending is projected to reach %.2f. Take immediate action to reduce expenses.", predicted);
            } else if ("MEDIUM".equals(risk)) {
                recommendation = String.format(
                        "Spending is on an upward trend. Consider setting stricter budgets for %s.", futureMonth);
            } else {
                recommendation = String.format(
                        "Spending appears stable for %s. Good financial discipline!", futureMonth);
            }

            predictions.add(MonthPredictionDTO.builder()
                    .month(futureMonth.toString())
                    .predictedAmount(round(predicted))
                    .riskLevel(risk)
                    .recommendation(recommendation)
                    .build());
        }

        List<SavingTipDTO> tips = buildPredictionTips(trendPercent, predictions);
        tips = enhanceTipsWithNNPredictions(tips, getNNPredictions(userId), NN_IDX_PREDICTIONS);

        return PredictionDTO.builder()
                .currentMonthSpending(round(currentMonthSpending))
                .trendPercent(round(trendPercent))
                .predictions(predictions)
                .tips(tips)
                .build();
    }

    private List<SavingTipDTO> buildPredictionTips(double trendPercent, List<MonthPredictionDTO> predictions) {
        List<SavingTipDTO> tips = new ArrayList<>();

        if (trendPercent > 10) {
            tips.add(SavingTipDTO.builder()
                    .title("Upward Spending Trend Detected")
                    .message(String.format(
                            "Your spending is increasing by approximately %.1f%% month-over-month. " +
                            "If this continues, you may face financial strain in the coming months.",
                            trendPercent))
                    .priority("HIGH")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Review and tighten your budget categories",
                            "Identify the categories driving the increase",
                            "Set a hard monthly spending cap",
                            "Schedule a monthly budget review"
                    ))
                    .build());
        } else if (trendPercent > 0) {
            tips.add(SavingTipDTO.builder()
                    .title("Slight Spending Increase")
                    .message(String.format(
                            "Your spending has increased by %.1f%% recently. Monitor this trend closely.",
                            trendPercent))
                    .priority("MEDIUM")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Review recent expenses for unnecessary spending",
                            "Compare this month's categories to last month"
                    ))
                    .build());
        } else {
            tips.add(SavingTipDTO.builder()
                    .title("Spending Trend is Stable or Improving")
                    .message("Your spending trend is stable or decreasing. Keep up the great work!")
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Consider channeling the savings into an emergency fund or investment"
                    ))
                    .build());
        }

        boolean hasHighRisk = predictions.stream().anyMatch(p -> "HIGH".equals(p.getRiskLevel()));
        if (hasHighRisk) {
            tips.add(SavingTipDTO.builder()
                    .title("High Risk Month Forecast")
                    .message("At least one of the next 3 months is projected to be high risk. Plan ahead now.")
                    .priority("HIGH")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Pre-plan your expenses for the high-risk month",
                            "Reduce discretionary spending now to build a buffer",
                            "Identify fixed costs you cannot avoid and plan around them"
                    ))
                    .build());
        }

        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type 5: Anomaly Detection & Alerts
    // ──────────────────────────────────────────────────────────────────────────

    public AnomalyDTO detectAnomalies(String userId) {
        List<Expense> allExpenses = expenseRepository.findByUserId(userId);
        YearMonth currentMonth = YearMonth.now();
        List<Expense> recentExpenses = getMonthlyExpenses(userId, currentMonth);

        // Calculate average amount per category across all history
        Map<String, Double> avgByCategory = allExpenses.stream()
                .filter(e -> e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getCategory()).orElse("Other").toLowerCase(Locale.ROOT),
                        Collectors.averagingDouble(e -> toDouble(e.getAmount()))
                ));

        double overallAvg = allExpenses.stream()
                .mapToDouble(e -> toDouble(e.getAmount())).average().orElse(0);

        List<AnomalyItemDTO> anomalies = recentExpenses.stream()
                .filter(e -> e.getAmount() != null)
                .filter(e -> {
                    String catKey = Optional.ofNullable(e.getCategory()).orElse("Other").toLowerCase(Locale.ROOT);
                    double avg = avgByCategory.getOrDefault(catKey, overallAvg);
                    return avg > 0 && toDouble(e.getAmount()) > avg * 2.0;
                })
                .map(e -> {
                    String catKey = Optional.ofNullable(e.getCategory()).orElse("Other").toLowerCase(Locale.ROOT);
                    double avg = avgByCategory.getOrDefault(catKey, overallAvg);
                    double amount = toDouble(e.getAmount());
                    double deviation = ((amount - avg) / avg) * 100;
                    return AnomalyItemDTO.builder()
                            .description(Optional.ofNullable(e.getDescription()).orElse("No description"))
                            .category(Optional.ofNullable(e.getCategory()).orElse("Other"))
                            .amount(round(amount))
                            .averageForCategory(round(avg))
                            .deviationPercent(round(deviation))
                            .date(e.getDate())
                            .suggestion(buildAnomalySuggestion(e.getCategory(), amount, avg))
                            .build();
                })
                .sorted(Comparator.comparingDouble(AnomalyItemDTO::getDeviationPercent).reversed())
                .collect(Collectors.toList());

        double totalAnomalyAmount = anomalies.stream().mapToDouble(AnomalyItemDTO::getAmount).sum();

        List<SavingTipDTO> tips = buildAnomalyTips(anomalies, totalAnomalyAmount);
        tips = enhanceTipsWithNNPredictions(tips, getNNPredictions(userId), NN_IDX_ANOMALIES);

        return AnomalyDTO.builder()
                .anomalyCount(anomalies.size())
                .totalAnomalyAmount(round(totalAnomalyAmount))
                .anomalies(anomalies)
                .tips(tips)
                .build();
    }

    private String buildAnomalySuggestion(String category, double amount, double avg) {
        String cat = Optional.ofNullable(category).orElse("Other");
        return String.format(
                "This %s expense (%.2f) is %.0f%% above your usual average (%.2f) for this category. " +
                "Consider if this was a planned expense, and if not, look for alternatives next time.",
                cat, amount, ((amount - avg) / avg) * 100, avg);
    }

    private List<SavingTipDTO> buildAnomalyTips(List<AnomalyItemDTO> anomalies, double totalAmount) {
        List<SavingTipDTO> tips = new ArrayList<>();
        if (anomalies.isEmpty()) {
            tips.add(SavingTipDTO.builder()
                    .title("No Spending Anomalies Detected")
                    .message("All your transactions this month are within normal ranges. Great consistency!")
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of("Keep tracking your expenses to maintain this consistency"))
                    .build());
        } else {
            tips.add(SavingTipDTO.builder()
                    .title(anomalies.size() + " Unusual Transaction(s) Detected")
                    .message(String.format(
                            "Found %d unusually large transactions totalling %.2f this month. " +
                            "These are significantly above your normal spending patterns.",
                            anomalies.size(), totalAmount))
                    .priority(anomalies.size() > 3 ? "HIGH" : "MEDIUM")
                    .potentialSavings(round(totalAmount * 0.3))
                    .actionItems(List.of(
                            "Review each flagged transaction to confirm it was necessary",
                            "If any were impulse purchases, consider returning them",
                            "Plan large purchases in advance to avoid surprise spikes",
                            "Set category-level alerts to catch high spending early"
                    ))
                    .build());
        }
        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type 6: Category-Specific Deep Dive
    // ──────────────────────────────────────────────────────────────────────────

    public CategoryAnalysisDTO analyzeCategoryDeepDive(String userId, String categoryName) {
        YearMonth currentMonth = YearMonth.now();
        List<Expense> allCategoryExpenses = expenseRepository
                .findByUserIdAndCategory(userId, categoryName);

        List<Expense> monthlyExpenses = allCategoryExpenses.stream()
                .filter(e -> e.getDate() != null
                        && YearMonth.from(e.getDate()).equals(currentMonth))
                .collect(Collectors.toList());

        double totalSpent = sumExpenses(monthlyExpenses);
        double budget = budgetRepository
                .findByUserIdAndCategoryIgnoreCase(userId, categoryName)
                .map(b -> b.getAmount() != null ? b.getAmount().doubleValue() : 0.0)
                .orElse(0.0);

        double budgetUsage = budget > 0 ? (totalSpent / budget) * 100 : 0;
        int txCount = monthlyExpenses.size();
        double avgTx = txCount > 0 ? totalSpent / txCount : 0;
        double maxTx = monthlyExpenses.stream().mapToDouble(e -> toDouble(e.getAmount())).max().orElse(0);
        double minTx = monthlyExpenses.stream().mapToDouble(e -> toDouble(e.getAmount())).min().orElse(0);

        // Weekly breakdown
        Map<String, Double> byWeek = new LinkedHashMap<>();
        for (int week = 1; week <= 5; week++) {
            final int w = week;
            int startDay = (w - 1) * 7 + 1;
            int endDay = Math.min(w * 7, currentMonth.lengthOfMonth());
            if (startDay > currentMonth.lengthOfMonth()) break;
            double weekTotal = monthlyExpenses.stream()
                    .filter(e -> e.getDate() != null
                            && e.getDate().getDayOfMonth() >= startDay
                            && e.getDate().getDayOfMonth() <= endDay)
                    .mapToDouble(e -> toDouble(e.getAmount())).sum();
            if (weekTotal > 0) {
                byWeek.put("Week " + w, round(weekTotal));
            }
        }

        List<SavingTipDTO> tips = buildCategoryTips(categoryName, totalSpent, budget, budgetUsage, avgTx, txCount);
        tips = enhanceTipsWithNNPredictions(tips, getNNPredictions(userId), NN_IDX_CATEGORY);

        return CategoryAnalysisDTO.builder()
                .categoryName(categoryName)
                .totalSpent(round(totalSpent))
                .budget(round(budget))
                .budgetUsagePercent(round(budgetUsage))
                .transactionCount(txCount)
                .avgTransactionAmount(round(avgTx))
                .maxTransactionAmount(round(maxTx))
                .minTransactionAmount(round(minTx))
                .spendingByWeek(byWeek)
                .tips(tips)
                .build();
    }

    private List<SavingTipDTO> buildCategoryTips(String category, double spent, double budget,
                                                  double usagePct, double avgTx, int txCount) {
        List<SavingTipDTO> tips = new ArrayList<>();

        if (budget > 0 && usagePct >= 100) {
            tips.add(SavingTipDTO.builder()
                    .title("Budget Exceeded for " + category)
                    .message(String.format(
                            "You have spent %.2f on %s this month, exceeding your budget of %.2f by %.2f.",
                            spent, category, budget, spent - budget))
                    .priority("HIGH")
                    .potentialSavings(round(spent - budget))
                    .actionItems(List.of(
                            "Pause all non-essential " + category + " purchases for the rest of the month",
                            "Review recent " + category + " transactions and identify any that were unnecessary",
                            "Consider increasing your budget if this category consistently exceeds the limit"
                    ))
                    .build());
        } else if (budget > 0 && usagePct >= 80) {
            tips.add(SavingTipDTO.builder()
                    .title("Approaching " + category + " Budget Limit")
                    .message(String.format(
                            "You have used %.1f%% of your %s budget. Only %.2f remaining for the month.",
                            usagePct, category, budget - spent))
                    .priority("MEDIUM")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Limit " + category + " spending for the rest of the month",
                            "Prioritize only essential " + category + " purchases"
                    ))
                    .build());
        }

        if (txCount > 10) {
            tips.add(SavingTipDTO.builder()
                    .title("Frequent " + category + " Purchases")
                    .message(String.format(
                            "You made %d transactions in %s this month (average %.2f each). " +
                            "Consolidating purchases can save time and money.",
                            txCount, category, avgTx))
                    .priority("MEDIUM")
                    .potentialSavings(round(avgTx * 0.2))
                    .actionItems(List.of(
                            "Batch your " + category + " purchases to reduce frequency",
                            "Buy in bulk where possible to reduce cost per unit",
                            "Use a shopping list to avoid repeat trips"
                    ))
                    .build());
        }

        if (tips.isEmpty()) {
            tips.add(SavingTipDTO.builder()
                    .title(category + " Spending is Under Control")
                    .message(String.format(
                            "Your %s spending (%.2f) is within expected limits. %s",
                            category, spent,
                            budget > 0 ? String.format("%.1f%% of your %.2f budget used.", usagePct, budget) : ""))
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of("Continue monitoring this category monthly"))
                    .build());
        }

        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type 7: Financial Wellness Score
    // ──────────────────────────────────────────────────────────────────────────

    public WellnessScoreDTO calculateWellnessScore(String userId) {
        YearMonth currentMonth = YearMonth.now();
        List<Expense> monthlyExpenses = getMonthlyExpenses(userId, currentMonth);
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);

        double totalSpent = sumExpenses(monthlyExpenses);
        double totalBudget = sumBudgets(budgets);

        // 1. Spending Discipline (0-100): Are they within budget?
        int spendingDiscipline = calculateSpendingDisciplineScore(totalSpent, totalBudget, budgets, monthlyExpenses);

        // 2. Budget Adherence (0-100): How many categories are within budget?
        int budgetAdherence = calculateBudgetAdherenceScore(userId, currentMonth, budgets);

        // 3. Saving Rate (0-100): Based on how much is left vs budget
        int savingRate = calculateSavingRateScore(totalSpent, totalBudget);

        // 4. Financial Awareness (0-100): Do they have budgets set?
        int financialAwareness = calculateFinancialAwarenessScore(budgets, monthlyExpenses);

        // 5. Risk Management (0-100): No over-budget categories
        int riskManagement = calculateRiskManagementScore(userId, currentMonth, budgets);

        int overall = (spendingDiscipline + budgetAdherence + savingRate + financialAwareness + riskManagement) / 5;

        String label;
        String nextMilestone;
        int pointsToNext;
        if (overall >= 80) {
            label = "EXCELLENT";
            nextMilestone = "100 – Perfect Score";
            pointsToNext = 100 - overall;
        } else if (overall >= 60) {
            label = "GOOD";
            nextMilestone = "80 – Excellent";
            pointsToNext = 80 - overall;
        } else if (overall >= 40) {
            label = "FAIR";
            nextMilestone = "60 – Good";
            pointsToNext = 60 - overall;
        } else {
            label = "POOR";
            nextMilestone = "40 – Fair";
            pointsToNext = 40 - overall;
        }

        List<SavingTipDTO> tips = buildWellnessTips(spendingDiscipline, budgetAdherence, savingRate,
                financialAwareness, riskManagement, overall);
        tips = enhanceTipsWithNNPredictions(tips, getNNPredictions(userId), NN_IDX_WELLNESS);

        return WellnessScoreDTO.builder()
                .overallScore(overall)
                .spendingDisciplineScore(spendingDiscipline)
                .budgetAdherenceScore(budgetAdherence)
                .savingRateScore(savingRate)
                .financialAwarenessScore(financialAwareness)
                .riskManagementScore(riskManagement)
                .scoreLabel(label)
                .nextMilestone(nextMilestone)
                .pointsToNextMilestone(pointsToNext)
                .tips(tips)
                .build();
    }

    private int calculateSpendingDisciplineScore(double totalSpent, double totalBudget,
                                                  List<Budget> budgets, List<Expense> expenses) {
        if (totalBudget <= 0) return 50;
        double ratio = totalSpent / totalBudget;
        if (ratio <= 0.7) return 100;
        if (ratio <= 0.8) return 90;
        if (ratio <= 0.9) return 80;
        if (ratio <= 1.0) return 65;
        if (ratio <= 1.1) return 45;
        if (ratio <= 1.2) return 30;
        return 10;
    }

    private int calculateBudgetAdherenceScore(String userId, YearMonth month, List<Budget> budgets) {
        if (budgets.isEmpty()) return 40;
        List<Expense> expenses = getMonthlyExpenses(userId, month);
        Map<String, Double> spentByCategory = groupByCategoryNormalized(expenses);
        long withinBudget = budgets.stream()
                .filter(b -> {
                    double spent = spentByCategory.getOrDefault(b.getCategory().toLowerCase(Locale.ROOT), 0.0);
                    double limit = b.getAmount() != null ? b.getAmount().doubleValue() : 0;
                    return spent <= limit;
                }).count();
        return (int) ((withinBudget * 100) / budgets.size());
    }

    private int calculateSavingRateScore(double totalSpent, double totalBudget) {
        if (totalBudget <= 0) return 30;
        double remaining = totalBudget - totalSpent;
        double savingRate = remaining / totalBudget;
        if (savingRate >= 0.3) return 100;
        if (savingRate >= 0.2) return 85;
        if (savingRate >= 0.1) return 70;
        if (savingRate >= 0) return 50;
        if (savingRate >= -0.1) return 30;
        return 10;
    }

    private int calculateFinancialAwarenessScore(List<Budget> budgets, List<Expense> expenses) {
        int score = 0;
        if (!budgets.isEmpty()) score += 50;
        if (budgets.size() >= 4) score += 20;
        if (!expenses.isEmpty()) score += 20;
        if (budgets.size() >= 6) score += 10;
        return Math.min(score, 100);
    }

    private int calculateRiskManagementScore(String userId, YearMonth month, List<Budget> budgets) {
        if (budgets.isEmpty()) return 50;
        List<Expense> expenses = getMonthlyExpenses(userId, month);
        Map<String, Double> spentByCategory = groupByCategoryNormalized(expenses);
        long exceededCount = budgets.stream()
                .filter(b -> {
                    double spent = spentByCategory.getOrDefault(b.getCategory().toLowerCase(Locale.ROOT), 0.0);
                    double limit = b.getAmount() != null ? b.getAmount().doubleValue() : 0;
                    return spent > limit;
                }).count();
        if (exceededCount == 0) return 100;
        if (exceededCount == 1) return 70;
        if (exceededCount == 2) return 50;
        return Math.max(10, 50 - (int) (exceededCount * 10));
    }

    private List<SavingTipDTO> buildWellnessTips(int spendingDiscipline, int budgetAdherence,
                                                  int savingRate, int financialAwareness,
                                                  int riskManagement, int overall) {
        List<SavingTipDTO> tips = new ArrayList<>();

        int minScore = Math.min(Math.min(Math.min(Math.min(spendingDiscipline, budgetAdherence), savingRate),
                financialAwareness), riskManagement);

        if (financialAwareness < 60) {
            tips.add(SavingTipDTO.builder()
                    .title("Set Up Category Budgets")
                    .message("Your Financial Awareness score is low. Setting budgets for all spending categories will help you gain control.")
                    .priority("HIGH")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Create budgets for all 8 spending categories",
                            "Review your spending history to determine realistic limits",
                            "Start with your top 3 spending categories"
                    ))
                    .build());
        }

        if (savingRate < 50) {
            tips.add(SavingTipDTO.builder()
                    .title("Improve Your Saving Rate")
                    .message("Your saving rate score is below average. Try to save at least 10-20% of your monthly budget.")
                    .priority("HIGH")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Set a savings target at the start of each month",
                            "Automatically transfer a fixed amount to savings on payday",
                            "Review and reduce discretionary spending categories"
                    ))
                    .build());
        }

        if (riskManagement < 70) {
            tips.add(SavingTipDTO.builder()
                    .title("Reduce Over-Budget Categories")
                    .message("You have exceeded the budget in multiple categories, increasing financial risk.")
                    .priority("HIGH")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Identify the most over-budget categories and address them first",
                            "Consider adjusting budgets to more realistic amounts",
                            "Track daily spending to catch overruns early"
                    ))
                    .build());
        }

        if (overall >= 80) {
            tips.add(SavingTipDTO.builder()
                    .title("Excellent Financial Wellness!")
                    .message(String.format(
                            "Your overall wellness score of %d is excellent. You are managing your finances very well.", overall))
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Consider investing surplus savings",
                            "Build an emergency fund of 3-6 months expenses",
                            "Review your financial goals annually"
                    ))
                    .build());
        }

        if (tips.isEmpty()) {
            tips.add(SavingTipDTO.builder()
                    .title("Keep Working on Your Financial Health")
                    .message(String.format(
                            "Your wellness score is %d/100. Focus on the lowest-scoring areas to improve.", overall))
                    .priority("MEDIUM")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Focus on the dimension with the lowest score",
                            "Make one small financial improvement each week",
                            "Review this score monthly to track progress"
                    ))
                    .build());
        }

        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type 8: Comparison to Own History
    // ──────────────────────────────────────────────────────────────────────────

    public HistoryTrendDTO analyzeHistoryTrend(String userId) {
        List<Double> monthlyTotals = getMonthlyTotals(userId, 6);

        Map<String, Double> monthlySpending = new LinkedHashMap<>();
        YearMonth startMonth = YearMonth.now().minusMonths(5);
        for (int i = 0; i < monthlyTotals.size(); i++) {
            monthlySpending.put(startMonth.plusMonths(i).toString(), round(monthlyTotals.get(i)));
        }

        double avg = monthlyTotals.stream().mapToDouble(d -> d).average().orElse(0);
        double trend = calculateTrend(monthlyTotals);

        String direction;
        boolean unsustainable = false;
        if (trend > 5) {
            direction = "INCREASING";
            unsustainable = trend > 15;
        } else if (trend < -5) {
            direction = "DECREASING";
        } else {
            direction = "STABLE";
        }

        List<SavingTipDTO> tips = buildHistoryTips(trend, direction, unsustainable, avg);
        tips = enhanceTipsWithNNPredictions(tips, getNNPredictions(userId), NN_IDX_HISTORY);

        return HistoryTrendDTO.builder()
                .monthlySpending(monthlySpending)
                .avgMonthlySpending(round(avg))
                .trendPercent(round(trend))
                .trendDirection(direction)
                .isUnsustainable(unsustainable)
                .tips(tips)
                .build();
    }

    private List<SavingTipDTO> buildHistoryTips(double trendPercent, String direction,
                                                 boolean unsustainable, double avg) {
        List<SavingTipDTO> tips = new ArrayList<>();

        if (unsustainable) {
            tips.add(SavingTipDTO.builder()
                    .title("Unsustainable Spending Trend!")
                    .message(String.format(
                            "Your spending has been increasing at %.1f%% per month over the past 6 months. " +
                            "This rate is unsustainable and requires immediate attention.",
                            trendPercent))
                    .priority("HIGH")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Conduct a full budget audit immediately",
                            "Identify and eliminate the top 3 growing expense categories",
                            "Set a hard monthly spending cap and track it daily",
                            "Consider speaking with a financial advisor"
                    ))
                    .build());
        } else if ("INCREASING".equals(direction)) {
            tips.add(SavingTipDTO.builder()
                    .title("Gradual Spending Increase Over 6 Months")
                    .message(String.format(
                            "Your spending has been gradually increasing (%.1f%%/month trend). " +
                            "Take action before it becomes a larger problem.",
                            trendPercent))
                    .priority("MEDIUM")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Compare this month vs 6 months ago to find what changed",
                            "Look for new recurring expenses that have been added",
                            String.format("Try to reduce spending back to your 6-month average of %.2f", avg)
                    ))
                    .build());
        } else if ("DECREASING".equals(direction)) {
            tips.add(SavingTipDTO.builder()
                    .title("Great Progress – Spending Decreasing!")
                    .message(String.format(
                            "Your spending has been decreasing by %.1f%%/month. Excellent financial discipline!",
                            Math.abs(trendPercent)))
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Keep up the great work!",
                            "Channel the freed-up money into savings or investments",
                            "Set a new, lower monthly spending target"
                    ))
                    .build());
        } else {
            tips.add(SavingTipDTO.builder()
                    .title("Stable Spending Pattern")
                    .message("Your spending has been consistent over the past 6 months. Good financial stability!")
                    .priority("LOW")
                    .potentialSavings(0)
                    .actionItems(List.of(
                            "Review if there are areas where you can reduce spending further",
                            "Consider using the consistency to start an investment plan"
                    ))
                    .build());
        }

        return tips;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Overview: summary of all 8 types
    // ──────────────────────────────────────────────────────────────────────────

    public TipsOverviewDTO getOverview(String userId) {
        SpendingPatternDTO pattern = analyzeSpendingPattern(userId);
        BehavioralAnalysisDTO behavioral = analyzeBehavior(userId);
        BenchmarkingDTO benchmarking = analyzeBenchmarking(userId);
        PredictionDTO predictions = analyzePredictions(userId);
        AnomalyDTO anomalies = detectAnomalies(userId);
        WellnessScoreDTO wellness = calculateWellnessScore(userId);
        HistoryTrendDTO history = analyzeHistoryTrend(userId);

        List<TipSummaryDTO> summaries = new ArrayList<>();

        summaries.add(buildSummary("SPENDING_PATTERN", "Spending Pattern Analysis",
                String.format("Total this month: %.2f (%.1f%% of budget)",
                        pattern.getTotalMonthlySpending(), pattern.getBudgetUsagePercent()),
                topPriority(pattern.getTips()), pattern.getTotalPotentialSavings()));

        summaries.add(buildSummary("BEHAVIORAL", "Behavioral Patterns",
                String.format("Peak day: %s, Impulse transactions: %d",
                        behavioral.getPeakSpendingDay(), behavioral.getImpulseTransactionCount()),
                topPriority(behavioral.getTips()), 0));

        summaries.add(buildSummary("BENCHMARKING", "Spending Benchmarks",
                String.format("%d categories above average, %d strengths",
                        benchmarking.getWeaknesses().size(), benchmarking.getStrengths().size()),
                topPriority(benchmarking.getTips()), 0));

        double predictedSavings = predictions.getPredictions().isEmpty() ? 0
                : predictions.getPredictions().get(0).getPredictedAmount();
        summaries.add(buildSummary("PREDICTIONS", "Spending Predictions",
                String.format("Trend: %+.1f%%, Next month forecast: %.2f",
                        predictions.getTrendPercent(), predictedSavings),
                topPriority(predictions.getTips()), 0));

        summaries.add(buildSummary("ANOMALIES", "Anomaly Alerts",
                String.format("%d unusual transactions detected (total %.2f)",
                        anomalies.getAnomalyCount(), anomalies.getTotalAnomalyAmount()),
                topPriority(anomalies.getTips()), round(anomalies.getTotalAnomalyAmount() * 0.3)));

        summaries.add(buildSummary("WELLNESS_SCORE", "Financial Wellness Score",
                String.format("Overall score: %d/100 (%s)",
                        wellness.getOverallScore(), wellness.getScoreLabel()),
                topPriority(wellness.getTips()), 0));

        summaries.add(buildSummary("HISTORY_TREND", "6-Month History Trend",
                String.format("Trend: %s (%+.1f%%/month)",
                        history.getTrendDirection(), history.getTrendPercent()),
                topPriority(history.getTips()), 0));

        double totalSavings = summaries.stream().mapToDouble(TipSummaryDTO::getPotentialSavings).sum();
        int totalTips = summaries.stream()
                .mapToInt(s -> "HIGH".equals(s.getPriority()) ? 2 : 1)
                .sum();

        return TipsOverviewDTO.builder()
                .totalTipsCount(totalTips)
                .totalPotentialSavings(round(totalSavings))
                .wellnessScore(wellness.getOverallScore())
                .summaries(summaries)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────────────────────────────────────

    private List<Expense> getMonthlyExpenses(String userId, YearMonth month) {
        return expenseRepository.findByUserIdAndDateBetween(
                userId, month.atDay(1), month.atEndOfMonth());
    }

    private List<Double> getMonthlyTotals(String userId, int months) {
        List<Double> totals = new ArrayList<>();
        YearMonth startMonth = YearMonth.now().minusMonths(months - 1);
        for (int i = 0; i < months; i++) {
            YearMonth month = startMonth.plusMonths(i);
            List<Expense> expenses = getMonthlyExpenses(userId, month);
            totals.add(sumExpenses(expenses));
        }
        return totals;
    }

    private double calculateTrend(List<Double> values) {
        if (values.size() < 2) return 0;
        // Simple linear regression slope as percentage
        List<Double> nonZero = values.stream().filter(v -> v > 0).collect(Collectors.toList());
        if (nonZero.size() < 2) return 0;
        double first = nonZero.get(0);
        double last = nonZero.get(nonZero.size() - 1);
        int periods = nonZero.size() - 1;
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
                        Collectors.summingDouble(e -> toDouble(e.getAmount()))
                ));
    }

    private Map<String, Double> groupByCategoryNormalized(List<Expense> expenses) {
        return expenses.stream()
                .filter(e -> e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getCategory()).orElse("Other").toLowerCase(Locale.ROOT),
                        Collectors.summingDouble(e -> toDouble(e.getAmount()))
                ));
    }

    private List<CategorySpendingDTO> buildCategorySpendingList(
            Map<String, Double> spentByCategory,
            Map<String, Double> budgetByCategory,
            double totalSpent) {
        Set<String> allCategories = new LinkedHashSet<>(spentByCategory.keySet());
        allCategories.addAll(budgetByCategory.keySet().stream()
                .map(k -> capitalizeFirst(k))
                .collect(Collectors.toSet()));

        return allCategories.stream()
                .map(category -> {
                    double spent = spentByCategory.getOrDefault(category, 0.0);
                    double budget = budgetByCategory.getOrDefault(category.toLowerCase(Locale.ROOT), 0.0);
                    double pct = totalSpent > 0 ? (spent / totalSpent) * 100 : 0;
                    String status;
                    double overAmount = 0;
                    if (budget <= 0) {
                        status = "NO_BUDGET";
                    } else if (spent > budget) {
                        status = "EXCEEDED";
                        overAmount = spent - budget;
                    } else if (spent >= budget * 0.8) {
                        status = "NEAR_LIMIT";
                    } else {
                        status = "SAFE";
                    }
                    return new CategorySpendingDTO(
                            category,
                            BigDecimal.valueOf(round(spent)),
                            round(pct),
                            BigDecimal.valueOf(round(budget)),
                            status,
                            round(overAmount)
                    );
                })
                .filter(c -> c.getAmount().doubleValue() > 0 || c.getBudget().doubleValue() > 0)
                .sorted(Comparator.comparingDouble((CategorySpendingDTO c) -> c.getAmount().doubleValue()).reversed())
                .collect(Collectors.toList());
    }

    private TipSummaryDTO buildSummary(String type, String title, String summary,
                                        String priority, double savings) {
        return TipSummaryDTO.builder()
                .type(type)
                .title(title)
                .summary(summary)
                .priority(priority)
                .potentialSavings(savings)
                .build();
    }

    private String topPriority(List<SavingTipDTO> tips) {
        if (tips == null || tips.isEmpty()) return "LOW";
        if (tips.stream().anyMatch(t -> "HIGH".equals(t.getPriority()))) return "HIGH";
        if (tips.stream().anyMatch(t -> "MEDIUM".equals(t.getPriority()))) return "MEDIUM";
        return "LOW";
    }

    private SavingTipDTO noDataTip() {
        return SavingTipDTO.builder()
                .title("Not Enough Data")
                .message("Add more expenses to get personalized insights.")
                .priority("LOW")
                .potentialSavings(0)
                .actionItems(List.of("Start adding your daily expenses to receive AI-powered tips"))
                .build();
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Neural Network integration
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to load the trained model for this user and run inference on the
     * current month's features.  Returns {@code null} when no recent model exists,
     * the model cannot be loaded, or feature extraction fails.
     */
    double[] getNNPredictions(String userId) {
        if (modelTrainingService == null || featureExtractionService == null) {
            return null;
        }
        if (!modelTrainingService.hasRecentModel(userId, NN_MAX_AGE_DAYS)) {
            return null;
        }
        try {
            Optional<MultiLayerNetwork> networkOpt = modelTrainingService.loadModel(userId);
            if (networkOpt.isEmpty()) {
                return null;
            }
            double[] features = featureExtractionService.extractFeatures(userId, YearMonth.now());
            if (!featureExtractionService.validateFeatures(features)) {
                log.warn("Invalid NN features for user {} — falling back to rule-based", userId);
                return null;
            }
            // Normalise single row using the same min-max approach
            double[][] norm = featureExtractionService.normalizeFeatures(new double[][]{features});
            INDArray input  = Nd4j.create(norm);
            INDArray output = networkOpt.get().output(input, false);
            double[] result = new double[output.columns()];
            for (int i = 0; i < result.length; i++) {
                result[i] = output.getDouble(0, i);
            }
            log.debug("NN predictions for user {}: {}", userId, Arrays.toString(result));
            return result;
        } catch (Exception e) {
            log.warn("NN inference failed for user {}: {} — falling back to rule-based",
                    userId, e.getMessage());
            return null;
        }
    }

    /**
     * Enhances a list of tips using NN output for the given tip-type index.
     * If the NN confidence for this tip type is high (> 0.75) the first tip's
     * priority is elevated to HIGH and an AI-confidence note is appended.
     * Falls back gracefully when {@code nnOutput} is null.
     */
    List<SavingTipDTO> enhanceTipsWithNNPredictions(List<SavingTipDTO> tips, double[] nnOutput, int nnIndex) {
        if (nnOutput == null || nnIndex >= nnOutput.length || tips.isEmpty()) {
            return tips;
        }
        double confidence = nnOutput[nnIndex];
        if (confidence > 0.75) {
            // Upgrade first tip to HIGH priority and note the NN confidence
            SavingTipDTO first = tips.get(0);
            List<String> actions = new ArrayList<>(first.getActionItems() != null
                    ? first.getActionItems() : List.of());
            actions.add(String.format("AI model confidence: %.0f%% — this tip is particularly relevant for you",
                    confidence * 100));
            SavingTipDTO enhanced = SavingTipDTO.builder()
                    .title(first.getTitle())
                    .message(first.getMessage())
                    .priority("HIGH")
                    .potentialSavings(first.getPotentialSavings())
                    .actionItems(actions)
                    .build();
            List<SavingTipDTO> result = new ArrayList<>();
            result.add(enhanced);
            result.addAll(tips.subList(1, tips.size()));
            return result;
        }
        return tips;
    }
}
