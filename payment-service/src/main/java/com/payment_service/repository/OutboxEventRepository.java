package com.payment_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payment_service.entity.OutboxEvent;
import com.payment_service.enums.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status " +
           "AND e.retryCount < 5 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents(@Param("status") OutboxStatus status);
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'FAILED' " +
           "AND e.retryCount >= 5")
    List<OutboxEvent> findDeadLetterEvents();
    
    void deleteByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime before);
}