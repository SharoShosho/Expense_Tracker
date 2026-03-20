package com.expensetracker.controller;

import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseChangeLog;
import com.expensetracker.service.AuthenticatedUserService;
import com.expensetracker.service.DataManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-management")
public class DataManagementController {

    private static final Logger log = LoggerFactory.getLogger(DataManagementController.class);

    @Autowired
    private DataManagementService dataManagementService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE (Soft Delete)
    // ──────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Map<String, String>> softDeleteExpense(
            @PathVariable String expenseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        dataManagementService.softDeleteExpense(expenseId, userId);
        return ResponseEntity.ok(Map.of("message", "Expense soft deleted"));
    }

    @PostMapping("/expenses/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkSoftDelete(
            @RequestBody List<String> expenseIds,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        dataManagementService.bulkSoftDelete(expenseIds, userId);
        return ResponseEntity.ok(Map.of(
                "message", "Expenses soft deleted",
                "count", expenseIds.size()
        ));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT (Bulk Update)
    // ──────────────────────────────────────────────────────────────────────────

    @PutMapping("/expenses/bulk-update")
    public ResponseEntity<Map<String, Object>> bulkUpdate(
            @RequestBody List<Expense> updates,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        dataManagementService.bulkUpdateExpenses(updates, userId);
        return ResponseEntity.ok(Map.of(
                "message", "Expenses updated",
                "count", updates.size()
        ));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST (Restore)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/expenses/{expenseId}/restore")
    public ResponseEntity<Map<String, String>> restoreExpense(
            @PathVariable String expenseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        dataManagementService.restoreExpense(expenseId, userId);
        return ResponseEntity.ok(Map.of("message", "Expense restored"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET (Active expenses & change history)
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/expenses")
    public ResponseEntity<List<Expense>> getActiveExpenses(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(dataManagementService.getActiveExpenses(userId));
    }

    @GetMapping("/changes/history")
    public ResponseEntity<List<ExpenseChangeLog>> getChangeHistory(
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(dataManagementService.getChangeHistory(userId, limit));
    }
}
