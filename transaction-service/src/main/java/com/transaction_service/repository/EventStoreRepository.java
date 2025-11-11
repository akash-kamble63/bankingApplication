package com.transaction_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transaction_service.entity.TransactionEventStore;

public interface EventStoreRepository extends JpaRepository<TransactionEventStore, Long> {
	List<TransactionEventStore> findByAggregateIdOrderByVersionAsc(String aggregateId);

	@Query("SELECT MAX(e.version) FROM TransactionEventStore e WHERE e.aggregateId = :aggregateId")
	Optional<Long> findLatestVersion(@Param("aggregateId") String aggregateId);

	boolean existsByEventId(String eventId);

	List<TransactionEventStore> findByCorrelationIdOrderByTimestampAsc(String correlationId);
}
