package com.expensetracker.controller;

import com.expensetracker.model.UserBehaviorProfile;
import com.expensetracker.model.UserTipInteraction;
import com.expensetracker.repository.UserTipInteractionRepository;
import com.expensetracker.service.AuthenticatedUserService;
import com.expensetracker.service.PersonalizationEngine;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST controller for recording tip feedback and exposing personalization data.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/ai/tips/feedback} – record a tip interaction</li>
 *   <li>{@code GET  /api/ai/tips/personalization/profile} – get the user's behavior profile</li>
 *   <li>{@code GET  /api/ai/tips/personalization/multipliers} – get current personalization multipliers</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ai/tips")
public class TipFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(TipFeedbackController.class);

    @Autowired
    private PersonalizationEngine personalizationEngine;

    @Autowired
    private UserTipInteractionRepository interactionRepository;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/ai/tips/feedback
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Records whether the user followed a tip and updates their personalization scores.
     *
     * @param userDetails injected by Spring Security
     * @param request     feedback payload
     * @return the persisted interaction record
     */
    @PostMapping("/feedback")
    public ResponseEntity<UserTipInteraction> recordFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TipFeedbackRequest request) {

        String userId = authenticatedUserService.resolveUserId(userDetails);

        UserTipInteraction interaction = UserTipInteraction.builder()
                .userId(userId)
                .tipId(request.getTipId())
                .tipType(request.getTipType())
                .tipTitle(request.getTipTitle())
                .wasFollowed(request.isWasFollowed())
                .followDate(request.isWasFollowed() ? LocalDateTime.now() : null)
                .feedback(request.getFeedback())
                .build();

        UserTipInteraction saved = interactionRepository.save(interaction);
        log.debug("Saved tip interaction for user {}: tipType={} wasFollowed={}",
                userId, request.getTipType(), request.isWasFollowed());

        // Update personalization scores based on user behavior
        personalizationEngine.updatePersonalizationScores(
                userId, request.getTipType(), request.isWasFollowed());

        return ResponseEntity.ok(saved);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/ai/tips/personalization/profile
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the authenticated user's behavior profile, creating it if it does
     * not yet exist.
     */
    @GetMapping("/personalization/profile")
    public ResponseEntity<UserBehaviorProfile> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        UserBehaviorProfile profile = personalizationEngine.loadOrCreateProfile(userId);
        return ResponseEntity.ok(profile);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/ai/tips/personalization/multipliers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current personalization multipliers (0.5–1.5) for each tip type.
     */
    @GetMapping("/personalization/multipliers")
    public ResponseEntity<Map<String, Double>> getMultipliers(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = authenticatedUserService.resolveUserId(userDetails);
        Map<String, Double> multipliers = personalizationEngine.getPersonalizationMultipliers(userId);
        return ResponseEntity.ok(multipliers);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Request DTO
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Payload for {@code POST /api/ai/tips/feedback}.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipFeedbackRequest {
        private String tipId;
        private String tipType;
        private String tipTitle;
        private boolean wasFollowed;
        private String feedback;
    }
}
