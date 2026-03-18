package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {

    List<Expense> findByUserId(String userId);

    List<Expense> findByUserIdAndCategory(String userId, String category);

    List<Expense> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserIdAndDescriptionContainingIgnoreCase(String userId, String description);
}
