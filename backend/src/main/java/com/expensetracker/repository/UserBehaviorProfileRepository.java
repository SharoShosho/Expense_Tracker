package com.expensetracker.repository;

import com.expensetracker.model.UserBehaviorProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBehaviorProfileRepository extends MongoRepository<UserBehaviorProfile, String> {

    Optional<UserBehaviorProfile> findByUserId(String userId);

    void deleteByUserId(String userId);
}
