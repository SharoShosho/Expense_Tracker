package com.expensetracker.service;

import com.expensetracker.dto.BudgetDTO;
import com.expensetracker.dto.BudgetOverviewDTO;
import com.expensetracker.dto.BudgetStatusDTO;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final BigDecimal NEAR_LIMIT_RATIO = new BigDecimal("0.80");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    public List<BudgetDTO> getBudgets(String userId) {
        return budgetRepository.findByUserIdOrderByCategoryAsc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public BudgetDTO upsertBudget(String userId, BudgetDTO dto) {
        String normalizedCategory = normalizeCategory(dto.getCategory());
        Budget budget = budgetRepository.findByUserIdAndCategoryIgnoreCase(userId, normalizedCategory)
                .orElseGet(Budget::new);

        budget.setUserId(userId);
        budget.setCategory(normalizedCategory);
        budget.setAmount(dto.getAmount());

        return toDto(budgetRepository.save(budget));
    }

    public void deleteBudget(String userId, String category) {
        budgetRepository.findByUserIdAndCategoryIgnoreCase(userId, normalizeCategory(category))
                .ifPresent(budgetRepository::delete);
    }

    public BudgetOverviewDTO getBudgetOverview(String userId, YearMonth month) {
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);
        Map<String, BigDecimal> monthlySpentByCategory = getMonthlySpentByCategory(userId, month);

        List<BudgetStatusDTO> categories = new ArrayList<>();
        int nearLimitCount = 0;
        int exceededCount = 0;

        for (Budget budget : budgets) {
            BigDecimal budgetAmount = safeAmount(budget.getAmount());
            BigDecimal spentAmount = monthlySpentByCategory
                    .getOrDefault(normalizeKey(budget.getCategory()), BigDecimal.ZERO);
            BigDecimal remainingAmount = budgetAmount.subtract(spentAmount);
            BigDecimal usageRatio = calculateUsageRatio(spentAmount, budgetAmount);
            BigDecimal usagePercent = usageRatio.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
            String status = resolveStatus(usageRatio);

            if ("NEAR_LIMIT".equals(status)) {
                nearLimitCount++;
            }
            if ("EXCEEDED".equals(status)) {
                exceededCount++;
            }

            categories.add(new BudgetStatusDTO(
                    budget.getCategory(),
                    budgetAmount,
                    spentAmount,
                    remainingAmount,
                    usagePercent,
                    status
            ));
        }

        BigDecimal totalBudget = categories.stream()
                .map(BudgetStatusDTO::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = categories.stream()
                .map(BudgetStatusDTO::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BudgetOverviewDTO(
                month.toString(),
                totalBudget,
                totalSpent,
                nearLimitCount,
                exceededCount,
                categories
        );
    }

    private Map<String, BigDecimal> getMonthlySpentByCategory(String userId, YearMonth month) {
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(
                userId,
                month.atDay(1),
                month.atEndOfMonth()
        );

        return expenses.stream()
                .filter(expense -> expense.getAmount() != null)
                .collect(Collectors.groupingBy(
                        expense -> normalizeKey(expense.getCategory()),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
    }

    private BigDecimal calculateUsageRatio(BigDecimal spentAmount, BigDecimal budgetAmount) {
        if (budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.divide(budgetAmount, 6, RoundingMode.HALF_UP);
    }

    private String resolveStatus(BigDecimal usageRatio) {
        if (usageRatio.compareTo(BigDecimal.ONE) >= 0) {
            return "EXCEEDED";
        }
        if (usageRatio.compareTo(NEAR_LIMIT_RATIO) >= 0) {
            return "NEAR_LIMIT";
        }
        return "SAFE";
    }

    private BudgetDTO toDto(Budget budget) {
        BudgetDTO dto = new BudgetDTO();
        dto.setCategory(budget.getCategory());
        dto.setAmount(budget.getAmount());
        return dto;
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return Optional.ofNullable(amount).orElse(BigDecimal.ZERO);
    }

    private String normalizeCategory(String category) {
        return Optional.ofNullable(category)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse("Other");
    }

    private String normalizeKey(String category) {
        return normalizeCategory(category).toLowerCase(Locale.ROOT);
    }
}

