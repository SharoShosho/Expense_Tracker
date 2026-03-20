package com.expensetracker.repository;

import com.expensetracker.model.UserTipInteraction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTipInteractionRepository extends MongoRepository<UserTipInteraction, String> {

    List<UserTipInteraction> findByUserId(String userId);

    List<UserTipInteraction> findByUserIdAndTipType(String userId, String tipType);

    long countByUserIdAndTipTypeAndWasFollowed(String userId, String tipType, boolean wasFollowed);
}
