package com.expensetracker.controller;

import com.expensetracker.dto.BudgetDTO;
import com.expensetracker.dto.BudgetOverviewDTO;
import com.expensetracker.service.AuthenticatedUserService;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getBudgets(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(budgetService.getBudgets(userId));
    }

    @PutMapping
    public ResponseEntity<BudgetDTO> upsertBudget(@AuthenticationPrincipal UserDetails userDetails,
                                                  @Valid @RequestBody BudgetDTO dto) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        return ResponseEntity.ok(budgetService.upsertBudget(userId, dto));
    }

    @DeleteMapping("/{category}")
    public ResponseEntity<Void> deleteBudget(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable String category) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        budgetService.deleteBudget(userId, category);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<BudgetOverviewDTO> getBudgetStatus(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestParam(required = false) String month) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        YearMonth selectedMonth;
        try {
            selectedMonth = (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid month format. Use YYYY-MM");
        }
        return ResponseEntity.ok(budgetService.getBudgetOverview(userId, selectedMonth));
    }
}


