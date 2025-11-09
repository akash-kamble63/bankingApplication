package com.transaction_service.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.transaction_service.entity.TransactionEventStore;
import java.util.*;

public interface TransactionEventStoreRepository extends JpaRepository<TransactionEventStore, Long> {
    List<TransactionEventStore> findByAggregateIdOrderByVersionAsc(String aggregateId);
    
    List<TransactionEventStore> findByAggregateIdAndVersionGreaterThanEqualOrderByVersionAsc(
        String aggregateId, Long fromVersion);
    
    @Query("SELECT MAX(e.version) FROM TransactionEventStore e WHERE e.aggregateId = :aggregateId")
    Optional<Long> findLatestVersion(String aggregateId);
    
    boolean existsByEventId(String eventId);
}