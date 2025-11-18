package com.card_service.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.card_service.entity.CardTransaction;
import com.card_service.enums.CardTransactionStatus;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

	Optional<CardTransaction> findByTransactionReference(String transactionReference);

	Optional<CardTransaction> findByAuthorizationCode(String authorizationCode);

	@Query("SELECT ct FROM CardTransaction ct WHERE ct.cardId = :cardId " + "ORDER BY ct.createdAt DESC")
	Page<CardTransaction> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

	@Query("SELECT ct FROM CardTransaction ct WHERE ct.userId = :userId " + "ORDER BY ct.createdAt DESC")
	Page<CardTransaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

	@Query("SELECT SUM(ct.amount) FROM CardTransaction ct " + "WHERE ct.cardId = :cardId AND ct.status = 'APPROVED' "
			+ "AND ct.createdAt BETWEEN :start AND :end")
	BigDecimal sumTransactionAmount(@Param("cardId") Long cardId, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	@Query("SELECT COUNT(ct) FROM CardTransaction ct " + "WHERE ct.cardId = :cardId AND ct.status = :status "
			+ "AND ct.createdAt > :since")
	Long countRecentTransactions(@Param("cardId") Long cardId, @Param("status") CardTransactionStatus status,
			@Param("since") LocalDateTime since);
}
