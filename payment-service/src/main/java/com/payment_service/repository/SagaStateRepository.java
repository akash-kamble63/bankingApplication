package com.payment_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payment_service.entity.SagaState;
import com.payment_service.enums.SagaStatus;

public interface SagaStateRepository extends JpaRepository<SagaState, Long> {
	Optional<SagaState> findBySagaId(String sagaId);

	List<SagaState> findByStatus(SagaStatus status);

	@Query("SELECT s FROM SagaState s WHERE s.status IN ('STARTED', 'PROCESSING') " + "AND s.createdAt < :timeout")
	List<SagaState> findStaleSagas(@Param("timeout") LocalDateTime timeout);
}
