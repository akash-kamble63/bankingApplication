package com.account_service.repository;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.model.OutboxEvent;
import com.account_service.model.OutboxEvent.OutboxStatus;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' " +
           "AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= CURRENT_TIMESTAMP) " +
           "AND o.retryCount < o.maxRetries " +
           "ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents(@Param("limit") int limit);
    
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' " +
           "AND o.createdAt < :olderThan")
    List<OutboxEvent> findFailedEvents(@Param("olderThan") LocalDateTime olderThan);
    
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' " +
           "AND o.publishedAt < :olderThan")
    int cleanupPublishedEvents(@Param("olderThan") LocalDateTime olderThan);
    
    List<OutboxEvent> findByAggregateIdAndStatus(String aggregateId, OutboxStatus status);
    
    long countByStatus(OutboxStatus status);
}