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

       /**
        * Find failed events that are ready for retry
        */
       @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' " +
                     "AND o.retryCount < o.maxRetries " +
                     "AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now) " +
                     "ORDER BY o.createdAt ASC")
       List<OutboxEvent> findRetryableEvents(@Param("now") LocalDateTime now, @Param("limit") int limit);

       @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' " +
                     "AND o.createdAt < :olderThan")
       List<OutboxEvent> findFailedEvents(@Param("olderThan") LocalDateTime olderThan);

       @Modifying
       @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' " +
                     "AND o.publishedAt < :olderThan")
       int cleanupPublishedEvents(@Param("olderThan") LocalDateTime olderThan);

       List<OutboxEvent> findByAggregateIdAndStatus(String aggregateId, OutboxStatus status);

       long countByStatus(OutboxStatus status);

       /**
        * Find events by aggregate type and ID
        */
       List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

       /**
        * Delete old published events (for cleanup)
        */
       @Modifying
       @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' " +
                     "AND o.publishedAt < :cutoffDate")
       int deleteOldPublishedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

       /**
        * Find events by event type
        */
       List<OutboxEvent> findByEventType(String eventType);

       /**
        * Find events created after a specific timestamp
        */
       List<OutboxEvent> findByCreatedAtAfter(LocalDateTime timestamp);

       /**
        * Count permanently failed events (exceeded max retries)
        */
       @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.status = 'FAILED' " +
                     "AND o.retryCount >= o.maxRetries")
       long countPermanentlyFailedEvents();

       /**
        * Find events by topic
        */
       List<OutboxEvent> findByTopic(String topic);

}