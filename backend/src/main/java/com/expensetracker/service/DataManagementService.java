package com.expensetracker.service;

import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseChangeLog;
import com.expensetracker.model.ModelDataSyncState;
import com.expensetracker.repository.ExpenseChangeLogRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.ModelDataSyncStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages data lifecycle operations (soft delete, bulk update, restore) and
 * keeps the neural-network model in sync through a single central decision point.
 */
@Service
public class DataManagementService {

    private static final Logger log = LoggerFactory.getLogger(DataManagementService.class);

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseChangeLogRepository changeLogRepository;

    @Autowired
    private ModelDataSyncStateRepository syncStateRepository;

    @Autowired
    private ModelTrainingService modelTrainingService;

    @Value("${data.management.immediate-retrain-threshold:100}")
    private int immediateRetrainThreshold;

    @Value("${data.management.batch-retrain-threshold:50}")
    private int batchRetrainThreshold;

    // ──────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Soft-delete a single expense (marks as deleted without removing from DB).
     * Automatically evaluates whether model retraining is needed.
     */
    public void softDeleteExpense(String expenseId, String userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));

        if (!expense.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: expense " + expenseId + " does not belong to user");
        }

        expense.setDeleted(true);
        expense.setDeletedAt(LocalDateTime.now());
        expense.setDeletedBy(userId);
        expense.setUpdatedAt(LocalDateTime.now());
        expenseRepository.save(expense);

        logChange(userId, expenseId, "SOFT_DELETE", 1);
        checkAndTriggerRetraining(userId);

        log.info("Soft deleted expense {} for user {}", expenseId, userId);
    }

    /**
     * Bulk soft-delete — mark multiple expenses as deleted in one pass.
     */
    public void bulkSoftDelete(List<String> expenseIds, String userId) {
        List<Expense> expenses = expenseRepository.findAllById(expenseIds);

        expenses.forEach(e -> {
            if (!e.getUserId().equals(userId)) {
                throw new IllegalArgumentException(
                        "Unauthorized: expense " + e.getId() + " does not belong to user");
            }
        });

        LocalDateTime now = LocalDateTime.now();
        expenses.forEach(e -> {
            e.setDeleted(true);
            e.setDeletedAt(now);
            e.setDeletedBy(userId);
            e.setUpdatedAt(now);
        });

        expenseRepository.saveAll(expenses);

        logChange(userId, "BULK", "SOFT_DELETE", expenses.size());
        checkAndTriggerRetraining(userId);

        log.info("Bulk soft deleted {} expenses for user {}", expenses.size(), userId);
    }

    /**
     * Bulk update — update category, amount, date, or description on multiple expenses.
     */
    public void bulkUpdateExpenses(List<Expense> updates, String userId) {
        List<Expense> toUpdate = expenseRepository.findAllById(
                updates.stream().map(Expense::getId).collect(Collectors.toList()));

        toUpdate.forEach(existing -> {
            Expense update = updates.stream()
                    .filter(u -> u.getId().equals(existing.getId()))
                    .findFirst()
                    .orElse(null);

            if (update == null) return;
            if (!existing.getUserId().equals(userId)) {
                throw new IllegalArgumentException(
                        "Unauthorized: expense " + existing.getId() + " does not belong to user");
            }

            if (update.getCategory() != null)    existing.setCategory(update.getCategory());
            if (update.getAmount() != null)       existing.setAmount(update.getAmount());
            if (update.getDate() != null)         existing.setDate(update.getDate());
            if (update.getDescription() != null)  existing.setDescription(update.getDescription());
            existing.setUpdatedAt(LocalDateTime.now());
        });

        expenseRepository.saveAll(toUpdate);

        logChange(userId, "BULK", "BULK_UPDATE", toUpdate.size());
        checkAndTriggerRetraining(userId);

        log.info("Bulk updated {} expenses for user {}", toUpdate.size(), userId);
    }

    /**
     * Restore a soft-deleted expense.
     */
    public void restoreExpense(String expenseId, String userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));

        if (!expense.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: expense " + expenseId + " does not belong to user");
        }

        if (!expense.isDeleted()) {
            throw new IllegalArgumentException("Expense " + expenseId + " is not deleted");
        }

        expense.setDeleted(false);
        expense.setDeletedAt(null);
        expense.setDeletedBy(null);
        expense.setUpdatedAt(LocalDateTime.now());
        expenseRepository.save(expense);

        logChange(userId, expenseId, "RESTORE", 1);
        checkAndTriggerRetraining(userId);

        log.info("Restored expense {} for user {}", expenseId, userId);
    }

    /**
     * Get all non-deleted expenses for a user.
     */
    public List<Expense> getActiveExpenses(String userId) {
        return expenseRepository.findByUserIdAndIsDeletedFalse(userId);
    }

    /**
     * Get the most recent change-log entries for a user.
     */
    public List<ExpenseChangeLog> getChangeHistory(String userId, int limit) {
        return changeLogRepository.findByUserIdOrderByChangedAtDesc(userId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CORE DECISION LOGIC — ONE CENTRAL PLACE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Single decision point: evaluates the number of recent changes and decides
     * whether to trigger immediate retraining, flag for batch retraining, or do nothing.
     */
    private void checkAndTriggerRetraining(String userId) {
        ModelDataSyncState syncState = syncStateRepository.findByUserId(userId)
                .orElseGet(() -> ModelDataSyncState.builder()
                        .userId(userId)
                        .pendingChangeCount(0)
                        .createdAt(LocalDateTime.now())
                        .build());

        long recentChanges = changeLogRepository.countByUserIdAndChangedAtAfter(
                userId, LocalDateTime.now().minusHours(24));

        syncState.setPendingChangeCount((int) recentChanges);
        syncState.setLastChangeAt(LocalDateTime.now());

        if (recentChanges >= immediateRetrainThreshold) {
            syncState.setNeedsImmediateRetrain(true);
            syncStateRepository.save(syncState);

            log.info("User {} has {} changes — IMMEDIATE RETRAIN triggered", userId, recentChanges);
            modelTrainingService.startTraining(userId);

            // Mark the most recent change-log entry to reflect that retraining was triggered
            changeLogRepository.findFirstByUserIdOrderByChangedAtDesc(userId).ifPresent(entry -> {
                entry.setRetrainingTriggered(true);
                entry.setRetrainingTriggeredAt(LocalDateTime.now());
                changeLogRepository.save(entry);
            });

        } else if (recentChanges >= batchRetrainThreshold) {
            syncState.setNeedsImmediateRetrain(false);
            syncStateRepository.save(syncState);

            log.info("User {} has {} changes — BATCH RETRAIN flagged for scheduled run", userId, recentChanges);

        } else {
            syncState.setNeedsImmediateRetrain(false);
            syncStateRepository.save(syncState);
        }
    }

    /**
     * Record a data-change event for audit and retrain-decision purposes.
     */
    private void logChange(String userId, String expenseId, String operationType, int recordCount) {
        ExpenseChangeLog changeLog = ExpenseChangeLog.builder()
                .userId(userId)
                .expenseId(expenseId)
                .operationType(operationType)
                .recordCount(recordCount)
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .retrainingTriggered(false)
                .build();

        changeLogRepository.save(changeLog);
    }
}
