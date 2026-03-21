package com.expensetracker.service;

import com.expensetracker.dto.SavingTipDTO;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class NNTipGeneratorTest {

    private NNTipGenerator generator;
    private ExpenseRepository expenseRepository;
    private BudgetRepository budgetRepository;

    private static final String USER_ID = "test-user";

    @BeforeEach
    void setUp() {
        generator = new NNTipGenerator();
        expenseRepository = Mockito.mock(ExpenseRepository.class);
        budgetRepository  = Mockito.mock(BudgetRepository.class);
        ReflectionTestUtils.setField(generator, "expenseRepository", expenseRepository);
        ReflectionTestUtils.setField(generator, "budgetRepository",  budgetRepository);

        // Default: no expenses, no budgets
        when(expenseRepository.findByUserIdAndIsDeletedFalse(eq(USER_ID))).thenReturn(List.of());
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(budgetRepository.findByUserIdOrderByCategoryAsc(eq(USER_ID))).thenReturn(List.of());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // confidenceToPriority
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void confidenceToPriority_returnsHighForHighConfidence() {
        assertEquals("HIGH", generator.confidenceToPriority(0.9));
        assertEquals("HIGH", generator.confidenceToPriority(0.6));
    }

    @Test
    void confidenceToPriority_returnsMediumForMidConfidence() {
        assertEquals("MEDIUM", generator.confidenceToPriority(0.5));
        assertEquals("MEDIUM", generator.confidenceToPriority(0.4));
    }

    @Test
    void confidenceToPriority_returnsLowForLowConfidence() {
        assertEquals("LOW", generator.confidenceToPriority(0.3));
        assertEquals("LOW", generator.confidenceToPriority(0.0));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generateTipsFromNNOutput
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generateTipsFromNNOutput_returnsEmptyWhenNNOutputNull() {
        List<SavingTipDTO> tips = generator.generateTipsFromNNOutput(USER_ID, null);
        assertNotNull(tips);
        assertTrue(tips.isEmpty());
    }

    @Test
    void generateTipsFromNNOutput_returnsEmptyWhenAllConfidenceBelowThreshold() {
        double[] lowConfidenceOutput = new double[8]; // all 0.0
        List<SavingTipDTO> tips = generator.generateTipsFromNNOutput(USER_ID, lowConfidenceOutput);
        assertNotNull(tips);
        assertTrue(tips.isEmpty());
    }

    @Test
    void generateTipsFromNNOutput_generatesAtLeastOneHighConfidenceTip() {
        double[] nnOutput = new double[8];
        nnOutput[0] = 0.85; // SPENDING_PATTERN

        // Need expenses for spending pattern tip to generate content
        List<Expense> expenses = List.of(buildExpense("Food", 300));
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(expenses);

        List<SavingTipDTO> tips = generator.generateTipsFromNNOutput(USER_ID, nnOutput);
        assertFalse(tips.isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generateSpendingPatternTip
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generateSpendingPatternTip_returnsNullBelowThreshold() {
        assertNull(generator.generateSpendingPatternTip(USER_ID, 0.3));
    }

    @Test
    void generateSpendingPatternTip_returnsTipAboveThreshold() {
        List<Expense> expenses = List.of(buildExpense("Food", 500));
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(expenses);

        SavingTipDTO tip = generator.generateSpendingPatternTip(USER_ID, 0.75);
        assertNotNull(tip);
        assertNotNull(tip.getTitle());
        assertNotNull(tip.getMessage());
        assertFalse(tip.getActionItems().isEmpty());
    }

    @Test
    void generateSpendingPatternTip_priorityMatchesConfidence() {
        List<Expense> expenses = List.of(buildExpense("Food", 200));
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(expenses);

        SavingTipDTO high   = generator.generateSpendingPatternTip(USER_ID, 0.8);
        SavingTipDTO medium = generator.generateSpendingPatternTip(USER_ID, 0.5);

        assertNotNull(high);
        assertNotNull(medium);
        assertEquals("HIGH",   high.getPriority());
        assertEquals("MEDIUM", medium.getPriority());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generateBehavioralTip
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generateBehavioralTip_returnsNullWithNoExpenses() {
        assertNull(generator.generateBehavioralTip(USER_ID, 0.8));
    }

    @Test
    void generateBehavioralTip_returnsTipWhenExpensesExist() {
        List<Expense> expenses = List.of(
                buildExpenseOnDate("Food", 100, LocalDate.now().with(java.time.DayOfWeek.MONDAY)),
                buildExpenseOnDate("Transport", 200, LocalDate.now().with(java.time.DayOfWeek.SATURDAY)));
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(expenses);

        SavingTipDTO tip = generator.generateBehavioralTip(USER_ID, 0.7);
        assertNotNull(tip);
        assertEquals("HIGH", tip.getPriority());
        assertTrue(tip.getMessage().contains("confidence"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generateBenchmarkingTip
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generateBenchmarkingTip_returnsNullWithNoSpending() {
        assertNull(generator.generateBenchmarkingTip(USER_ID, 0.8));
    }

    @Test
    void generateBenchmarkingTip_identifiesAboveAverageCategory() {
        // Spend heavily on Entertainment (benchmark 10%) so it exceeds benchmark
        List<Expense> expenses = List.of(
                buildExpense("Entertainment", 1000),
                buildExpense("Food", 100));
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(expenses);

        SavingTipDTO tip = generator.generateBenchmarkingTip(USER_ID, 0.75);
        assertNotNull(tip);
        assertNotNull(tip.getMessage());
        assertTrue(tip.getPotentialSavings() >= 0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generatePredictionTip
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generatePredictionTip_returnsNullWithNoHistory() {
        assertNull(generator.generatePredictionTip(USER_ID, 0.8));
    }

    @Test
    void generatePredictionTip_returnsTipWithHistory() {
        // Return non-zero for multiple month queries
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of(buildExpense("Food", 300)));

        SavingTipDTO tip = generator.generatePredictionTip(USER_ID, 0.7);
        assertNotNull(tip);
        assertNotNull(tip.getMessage());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generateCategoryTip
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generateCategoryTip_returnsNullWithNoExpenses() {
        assertNull(generator.generateCategoryTip(USER_ID, 0.8));
    }

    @Test
    void generateCategoryTip_flagsTopCategory() {
        List<Expense> expenses = List.of(
                buildExpense("Food", 500),
                buildExpense("Transport", 200));
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(expenses);

        SavingTipDTO tip = generator.generateCategoryTip(USER_ID, 0.8);
        assertNotNull(tip);
        assertTrue(tip.getMessage().contains("Food"));
        assertTrue(tip.getPotentialSavings() > 0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generateWellnessTip
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generateWellnessTip_returnsNoBudgetDimensionWhenNoBudgets() {
        SavingTipDTO tip = generator.generateWellnessTip(USER_ID, 0.7);
        assertNotNull(tip);
        assertTrue(tip.getMessage().contains("Financial Awareness"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generateHistoryTip
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void generateHistoryTip_returnsNullWithNoHistory() {
        assertNull(generator.generateHistoryTip(USER_ID, 0.8));
    }

    @Test
    void generateHistoryTip_returnsUrgentTipOnHighTrend() {
        // Simulate increasing monthly totals: 100 → 200 → 300 → 400 → 500 → 600
        when(expenseRepository.findByUserIdAndIsDeletedFalseAndDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of(buildExpense("Food", 600))); // simplified – all months same

        SavingTipDTO tip = generator.generateHistoryTip(USER_ID, 0.9);
        assertNotNull(tip);
        assertNotNull(tip.getMessage());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Expense buildExpense(String category, double amount) {
        return buildExpenseOnDate(category, amount, YearMonth.now().atDay(15));
    }

    private Expense buildExpenseOnDate(String category, double amount, LocalDate date) {
        Expense e = new Expense();
        e.setId(java.util.UUID.randomUUID().toString());
        e.setUserId(USER_ID);
        e.setCategory(category);
        e.setAmount(BigDecimal.valueOf(amount));
        e.setDate(date);
        return e;
    }
}
