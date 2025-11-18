package com.loan_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loan_service.entity.OutboxEvent;
import com.loan_service.enums.OutboxStatus;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

	@Query("SELECT e FROM OutboxEvent e WHERE e.status = :status "
			+ "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " + "ORDER BY e.createdAt ASC")
	List<OutboxEvent> findPendingEventsReadyForRetry(@Param("status") OutboxStatus status,
			@Param("now") LocalDateTime now);

	List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(String aggregateType, String aggregateId);

	long countByStatus(OutboxStatus status);

	@Modifying
	@Query("DELETE FROM OutboxEvent e WHERE e.status = :status " + "AND e.createdAt < :cutoffDate")
	int cleanupPublishedEvents(@Param("status") OutboxStatus status, @Param("cutoffDate") LocalDateTime cutoffDate);

	void deleteByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime cutoffDate);
}
