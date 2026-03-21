package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {

    List<Expense> findByUserId(String userId);

    List<Expense> findByUserIdAndCategory(String userId, String category);

    List<Expense> findByUserIdAndCategoryIgnoreCaseAndIsDeletedFalse(String userId, String category);

    List<Expense> findByUserIdAndCategoryIgnoreCaseAndIsDeletedFalseAndDateBetween(
            String userId, String category, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserIdAndDescriptionContainingIgnoreCase(String userId, String description);

    // Soft-delete aware queries
    List<Expense> findByUserIdAndIsDeletedFalse(String userId);

    List<Expense> findByUserIdAndIsDeletedTrue(String userId);

    List<Expense> findByUserIdAndIsDeletedFalseAndDateBetween(
            String userId, LocalDate startDate, LocalDate endDate);

    long countByUserIdAndIsDeletedFalseAndUpdatedAtAfter(String userId, LocalDateTime since);

    long countByUserIdAndIsDeletedTrue(String userId);
}
