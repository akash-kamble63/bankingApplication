package com.account_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.model.AccountEventStore;

public interface EventStoreRepository extends JpaRepository<AccountEventStore, Long> {
	// Get all events for an account (for replay/rebuild)
    List<AccountEventStore> findByAggregateIdOrderByVersionAsc(String aggregateId);
    
    // Get events from specific version
    List<AccountEventStore> findByAggregateIdAndVersionGreaterThanEqualOrderByVersionAsc(
        String aggregateId, Long fromVersion);
    
    // Get events within version range
    @Query("SELECT e FROM AccountEventStore e WHERE e.aggregateId = :aggregateId " +
           "AND e.version BETWEEN :fromVersion AND :toVersion " +
           "ORDER BY e.version ASC")
    List<AccountEventStore> findEventsByVersionRange(
        @Param("aggregateId") String aggregateId,
        @Param("fromVersion") Long fromVersion,
        @Param("toVersion") Long toVersion
    );
    
    // Get latest version number
    @Query("SELECT MAX(e.version) FROM AccountEventStore e WHERE e.aggregateId = :aggregateId")
    Optional<Long> findLatestVersion(@Param("aggregateId") String aggregateId);
    
    // Get events by type
    List<AccountEventStore> findByEventTypeOrderByTimestampDesc(String eventType);
    
    // Get events by date range
    List<AccountEventStore> findByAggregateIdAndTimestampBetweenOrderByVersionAsc(
        String aggregateId, LocalDateTime start, LocalDateTime end);
    
    // Check if event exists
    boolean existsByEventId(String eventId);
    
    // Get events by correlation ID (saga tracking)
    List<AccountEventStore> findByCorrelationIdOrderByTimestampAsc(String correlationId);
    
    // Count events
    long countByAggregateId(String aggregateId);
    
    // Get latest event
    @Query("SELECT e FROM AccountEventStore e WHERE e.aggregateId = :aggregateId " +
           "ORDER BY e.version DESC LIMIT 1")
    Optional<AccountEventStore> findLatestEvent(@Param("aggregateId") String aggregateId);
}
