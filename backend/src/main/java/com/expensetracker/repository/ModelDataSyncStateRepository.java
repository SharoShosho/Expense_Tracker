package com.expensetracker.repository;

import com.expensetracker.model.ModelDataSyncState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModelDataSyncStateRepository extends MongoRepository<ModelDataSyncState, String> {

    Optional<ModelDataSyncState> findByUserId(String userId);
}
