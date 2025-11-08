package com.account_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.model.SagaState;
import com.account_service.model.SagaState.SagaStatus;

public interface SagaRepository extends JpaRepository<SagaState, Long> {
Optional<SagaState> findBySagaId(String sagaId);
    
    List<SagaState> findByStatus(SagaStatus status);
    
    @Query("SELECT s FROM SagaState s WHERE s.status IN ('STARTED', 'PROCESSING') " +
           "AND s.updatedAt < :staleTime")
    List<SagaState> findStaleSagas(@Param("staleTime") LocalDateTime staleTime);
    
    @Query("SELECT s FROM SagaState s WHERE s.status = 'FAILED' " +
           "AND s.retryCount < s.maxRetries " +
           "AND s.updatedAt < :retryTime")
    List<SagaState> findFailedSagasForRetry(@Param("retryTime") LocalDateTime retryTime);
    
    long countByStatus(SagaStatus status);
    
    List<SagaState> findBySagaTypeAndStatus(String sagaType, SagaStatus status);
}
