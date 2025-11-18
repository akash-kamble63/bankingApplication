package com.card_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.card_service.entity.CardEventStore;

public interface EventStoreRepository extends JpaRepository<CardEventStore, Long> {

	List<CardEventStore> findByAggregateIdOrderByVersionAsc(String aggregateId);

	@Query("SELECT MAX(e.version) FROM CardEventStore e WHERE e.aggregateId = :aggregateId")
	Optional<Long> findLatestVersion(@Param("aggregateId") String aggregateId);

	List<CardEventStore> findByUserIdOrderByTimestampDesc(Long userId);

	List<CardEventStore> findByEventType(String eventType);

	@Query("SELECT e FROM CardEventStore e WHERE e.aggregateId = :aggregateId "
			+ "AND e.version BETWEEN :startVersion AND :endVersion " + "ORDER BY e.version ASC")
	List<CardEventStore> findEventsBetweenVersions(@Param("aggregateId") String aggregateId,
			@Param("startVersion") Long startVersion, @Param("endVersion") Long endVersion);
}
