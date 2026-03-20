package com.expensetracker.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "expenseChangeLogs")
public class ExpenseChangeLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String expenseId;
    private String operationType; // SOFT_DELETE, BULK_UPDATE, RESTORE
    private int recordCount;
    private LocalDateTime changedAt;
    private String changedBy;

    private boolean retrainingTriggered;
    private LocalDateTime retrainingTriggeredAt;
}
