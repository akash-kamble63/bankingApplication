package com.transaction_service.repository;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.transaction_service.entity.OutboxEvent;
import com.transaction_service.entity.OutboxEvent.OutboxStatus;
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long>{
	@Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' " +
	           "AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= CURRENT_TIMESTAMP) " +
	           "AND o.retryCount < o.maxRetries " +
	           "ORDER BY o.createdAt ASC")
	    List<OutboxEvent> findPendingEvents(int limit);
	    
	    @Modifying
	    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' " +
	           "AND o.publishedAt < :olderThan")
	    int cleanupPublishedEvents(LocalDateTime olderThan);
	    
	    List<OutboxEvent> findByAggregateIdAndStatus(String aggregateId, OutboxStatus status);

}
