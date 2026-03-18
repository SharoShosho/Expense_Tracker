package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseDTO;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
    }

    @PostMapping("/expenses")
    public ResponseEntity<Expense> createExpense(@AuthenticationPrincipal UserDetails userDetails,
                                                  @Valid @RequestBody ExpenseDTO dto) {
        String userId = getUserId(userDetails);
        Expense expense = expenseService.createExpense(userId, dto);
        return ResponseEntity.ok(expense);
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<Expense>> getExpenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = getUserId(userDetails);
        List<Expense> expenses = expenseService.getExpenses(userId, category, search, startDate, endDate);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/expenses/{id}")
    public ResponseEntity<Expense> getExpense(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable String id) {
        String userId = getUserId(userDetails);
        Expense expense = expenseService.getExpense(userId, id);
        return ResponseEntity.ok(expense);
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<Expense> updateExpense(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable String id,
                                                  @Valid @RequestBody ExpenseDTO dto) {
        String userId = getUserId(userDetails);
        Expense expense = expenseService.updateExpense(userId, id, dto);
        return ResponseEntity.ok(expense);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable String id) {
        String userId = getUserId(userDetails);
        expenseService.deleteExpense(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        Map<String, Object> stats = expenseService.getStatistics(userId);
        return ResponseEntity.ok(stats);
    }
}
