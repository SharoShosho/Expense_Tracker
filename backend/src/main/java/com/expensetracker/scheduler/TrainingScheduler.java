package com.expensetracker.scheduler;

import com.expensetracker.model.ModelDataSyncState;
import com.expensetracker.model.User;
import com.expensetracker.repository.ModelDataSyncStateRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.ModelTrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Periodically re-trains neural network models for all users.
 * The schedule is configurable; by default it runs every 7 days.
 * Training can be disabled entirely via {@code nn.training.enabled=false}.
 *
 * In addition to the staleness check, the scheduler respects the
 * {@link ModelDataSyncState#isNeedsImmediateRetrain()} flag set by
 * {@code DataManagementService} when significant data changes occur.
 */
@Component
@ConditionalOnProperty(name = "nn.training.enabled", havingValue = "true", matchIfMissing = true)
public class TrainingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrainingScheduler.class);

    @Value("${nn.training.schedule-days:7}")
    private int scheduleDays;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelTrainingService modelTrainingService;

    @Autowired
    private ModelDataSyncStateRepository modelDataSyncStateRepository;

    /**
     * Runs once per day and re-trains any user whose model is older than
     * {@code nn.training.schedule-days} days (or who has never been trained),
     * or whose sync state is flagged for immediate retraining.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledTraining() {
        log.info("Scheduled training check started (re-train threshold: {} days)", scheduleDays);

        List<User> allUsers;
        try {
            allUsers = userRepository.findAll();
        } catch (Exception e) {
            log.error("Could not load users for scheduled training: {}", e.getMessage(), e);
            return;
        }

        int triggered = 0;
        for (User user : allUsers) {
            try {
                Optional<ModelDataSyncState> syncStateOpt =
                        modelDataSyncStateRepository.findByUserId(user.getId());

                boolean needsImmediate = syncStateOpt
                        .map(ModelDataSyncState::isNeedsImmediateRetrain)
                        .orElse(false);

                boolean modelIsStale = !modelTrainingService.hasRecentModel(user.getId(), scheduleDays);

                if (needsImmediate || modelIsStale) {
                    log.info("Triggering training for user {} (needsImmediate={}, stale={})",
                            user.getId(), needsImmediate, modelIsStale);
                    modelTrainingService.startTraining(user.getId());
                    triggered++;

                    // Clear the immediate-retrain flag after scheduling
                    if (syncStateOpt.isPresent()) {
                        ModelDataSyncState state = syncStateOpt.get();
                        state.setNeedsImmediateRetrain(false);
                        state.setLastSyncedAt(LocalDateTime.now());
                        modelDataSyncStateRepository.save(state);
                    }
                }
            } catch (Exception e) {
                log.error("Scheduled training failed for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }

        log.info("Scheduled training check complete — triggered {} training job(s)", triggered);
    }
}
