package com.expensetracker.repository;

import com.expensetracker.model.ExpenseChangeLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseChangeLogRepository extends MongoRepository<ExpenseChangeLog, String> {

    List<ExpenseChangeLog> findByUserIdOrderByChangedAtDesc(String userId);

    long countByUserIdAndChangedAtAfter(String userId, LocalDateTime since);

    Optional<ExpenseChangeLog> findFirstByUserIdOrderByChangedAtDesc(String userId);
}
