package com.expensetracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Records a single interaction between a user and a personalized tip.
 * Used by {@link com.expensetracker.service.PersonalizationEngine} to
 * update engagement scores and train the personalization model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userTipInteraction")
public class UserTipInteraction {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String tipId;
    private String tipType;
    private String tipTitle;

    /** Whether the user acted on this tip. */
    private boolean wasFollowed;

    /** Date/time the user marked the tip as followed (may be null if ignored). */
    private LocalDateTime followDate;

    /** Optional free-text feedback from the user. */
    private String feedback;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
