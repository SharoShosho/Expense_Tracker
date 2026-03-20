package com.expensetracker.repository;

import com.expensetracker.model.ModelMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelMetadataRepository extends MongoRepository<ModelMetadata, String> {

    /** Return the most recently created metadata entry for a user. */
    Optional<ModelMetadata> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    List<ModelMetadata> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Optional<ModelMetadata> findByUserIdAndModelVersion(String userId, String modelVersion);

    void deleteByUserId(String userId);
}
