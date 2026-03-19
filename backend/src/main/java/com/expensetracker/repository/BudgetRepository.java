package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends MongoRepository<Budget, String> {
    List<Budget> findByUserIdOrderByCategoryAsc(String userId);

    Optional<Budget> findByUserIdAndCategoryIgnoreCase(String userId, String category);
}

