package com.expensetracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "budgets")
@CompoundIndex(def = "{'userId': 1, 'category': 1}", unique = true)
public class Budget {

    @Id
    private String id;

    private String userId;

    private String category;

    private BigDecimal amount;
}

