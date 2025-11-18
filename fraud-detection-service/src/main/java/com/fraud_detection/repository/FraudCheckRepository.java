package com.fraud_detection.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fraud_detection.entity.FraudCheck;
import com.fraud_detection.enums.FraudStatus;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {
    
    Optional<FraudCheck> findByTransactionId(String transactionId);
    
    List<FraudCheck> findByAccountIdAndCreatedAtAfter(String accountId, LocalDateTime after);
    
    List<FraudCheck> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime after);
    
    Page<FraudCheck> findByStatus(FraudStatus status, Pageable pageable);
    
    Page<FraudCheck> findByAccountId(String accountId, Pageable pageable);
    
    Page<FraudCheck> findByReviewedFalseAndStatus(FraudStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(f) FROM FraudCheck f WHERE f.accountId = :accountId " +
           "AND f.createdAt >= :startTime AND f.createdAt <= :endTime")
    Long countTransactionsByAccountInTimeRange(
        @Param("accountId") String accountId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM FraudCheck f WHERE f.accountId = :accountId " +
           "AND f.createdAt >= :startTime AND f.createdAt <= :endTime")
    BigDecimal sumAmountByAccountInTimeRange(
        @Param("accountId") String accountId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT f FROM FraudCheck f WHERE f.accountId = :accountId " +
           "ORDER BY f.createdAt DESC LIMIT 1")
    Optional<FraudCheck> findLatestByAccountId(@Param("accountId") String accountId);
    
    @Query("SELECT COUNT(f) FROM FraudCheck f WHERE f.status = :status " +
           "AND f.createdAt >= :startTime")
    Long countByStatusSince(
        @Param("status") FraudStatus status,
        @Param("startTime") LocalDateTime startTime
    );
    
    @Query("SELECT AVG(f.riskScore) FROM FraudCheck f WHERE f.createdAt >= :startTime")
    Double averageRiskScoreSince(@Param("startTime") LocalDateTime startTime);

}
