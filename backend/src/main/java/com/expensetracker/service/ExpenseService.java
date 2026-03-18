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
import java.util.stream.Collectors;

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
        List<Expense> expenses = expenseRepository.findByUserId(userId);

        if (category != null && !category.isBlank()) {
            expenses = expenses.stream()
                    .filter(e -> e.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }
        if (search != null && !search.isBlank()) {
            String lowerSearch = search.toLowerCase();
            expenses = expenses.stream()
                    .filter(e -> e.getDescription() != null && e.getDescription().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }
        if (startDate != null) {
            expenses = expenses.stream()
                    .filter(e -> !e.getDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }
        if (endDate != null) {
            expenses = expenses.stream()
                    .filter(e -> !e.getDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }
        return expenses;
    }

    public Expense getExpense(String userId, String id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        if (!expense.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Expense", id);
        }
        return expense;
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
