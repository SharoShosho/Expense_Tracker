package com.expensetracker.service;

import com.expensetracker.dto.ExpenseDTO;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public Expense createExpense(String userId, ExpenseDTO dto) {
        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setDescription(dto.getDescription());
        expense.setDate(dto.getDate());
        return expenseRepository.save(expense);
    }

    public List<Expense> getExpenses(String userId, String category, String search,
                                      LocalDate startDate, LocalDate endDate) {
        Predicate<Expense> combinedFilter = Stream.of(
                        categoryFilter(category),
                        searchFilter(search),
                        startDateFilter(startDate),
                        endDateFilter(endDate)
                )
                .flatMap(Optional::stream)
                .reduce(expense -> true, Predicate::and);

        return expenseRepository.findByUserId(userId).stream()
                .filter(combinedFilter)
                .collect(Collectors.toList());
    }

    public Expense getExpense(String userId, String id) {
        return expenseRepository.findById(id)
                .filter(expense -> expense.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private Optional<Predicate<Expense>> categoryFilter(String category) {
        return Optional.ofNullable(category)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> expense -> expense.getCategory().equalsIgnoreCase(value));
    }

    private Optional<Predicate<Expense>> searchFilter(String search) {
        return Optional.ofNullable(search)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .map(lowerSearch -> expense -> expense.getDescription() != null
                        && expense.getDescription().toLowerCase().contains(lowerSearch));
    }

    private Optional<Predicate<Expense>> startDateFilter(LocalDate startDate) {
        return Optional.ofNullable(startDate)
                .map(value -> expense -> !expense.getDate().isBefore(value));
    }

    private Optional<Predicate<Expense>> endDateFilter(LocalDate endDate) {
        return Optional.ofNullable(endDate)
                .map(value -> expense -> !expense.getDate().isAfter(value));
    }

    public Expense updateExpense(String userId, String id, ExpenseDTO dto) {
        Expense expense = getExpense(userId, id);
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setDescription(dto.getDescription());
        expense.setDate(dto.getDate());
        return expenseRepository.save(expense);
    }

    public void deleteExpense(String userId, String id) {
        Expense expense = getExpense(userId, id);
        expenseRepository.delete(expense);
    }

    public Map<String, Object> getStatistics(String userId) {
        List<Expense> expenses = expenseRepository.findByUserId(userId);

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Map<String, BigDecimal> byMonth = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDate().getYear() + "-" + String.format("%02d", e.getDate().getMonthValue()),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAmount", totalAmount);
        stats.put("totalCount", expenses.size());
        stats.put("byCategory", byCategory);
        stats.put("byMonth", byMonth);
        return stats;
    }
}
