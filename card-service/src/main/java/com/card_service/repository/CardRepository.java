package com.card_service.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.card_service.entity.Card;
import com.card_service.enums.CardStatus;

import jakarta.persistence.LockModeType;

public interface CardRepository extends JpaRepository<Card, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.cardToken = :token")
    Optional<Card> findByCardTokenForUpdate(@Param("token") String cardToken);
    
    Optional<Card> findByCardToken(String cardToken);
    
    Optional<Card> findByCardReference(String cardReference);
    
    Optional<Card> findByCardNumberHash(String cardNumberHash);
    
    @Query("SELECT c FROM Card c WHERE c.userId = :userId ORDER BY c.createdAt DESC")
    Page<Card> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT c FROM Card c WHERE c.userId = :userId AND c.status = :status")
    Page<Card> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") CardStatus status,
        Pageable pageable
    );
    
    @Query("SELECT c FROM Card c WHERE c.accountId = :accountId")
    List<Card> findByAccountId(@Param("accountId") Long accountId);
    
    @Query("SELECT COUNT(c) FROM Card c WHERE c.userId = :userId AND c.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CardStatus status);
    
    @Query("SELECT c FROM Card c WHERE c.status = 'ACTIVE' " +
           "AND c.lastResetDate < :today")
    List<Card> findCardsNeedingReset(@Param("today") LocalDate today);
    
    @Query("SELECT c FROM Card c WHERE c.status = 'PENDING_ACTIVATION' " +
           "AND c.activationExpiry < :now")
    List<Card> findExpiredPendingActivations(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Card c WHERE c.status = 'ACTIVE' " +
           "AND c.expiryYear = :year AND c.expiryMonth = :month")
    List<Card> findExpiringCards(@Param("year") Integer year, @Param("month") Integer month);
    
    @Modifying
    @Query("UPDATE Card c SET c.dailySpent = 0, c.dailyWithdrawals = 0, " +
           "c.lastResetDate = :today WHERE c.id IN :ids")
    void resetDailyLimits(@Param("ids") List<Long> cardIds, @Param("today") LocalDate today);
    
    @Modifying
    @Query("UPDATE Card c SET c.monthlySpent = 0 WHERE c.id IN :ids")
    void resetMonthlyLimits(@Param("ids") List<Long> cardIds);
}