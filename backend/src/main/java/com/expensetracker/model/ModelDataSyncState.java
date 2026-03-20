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
@Document(collection = "modelDataSyncState")
public class ModelDataSyncState {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private int pendingChangeCount;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastChangeAt;
    private boolean needsImmediateRetrain;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
