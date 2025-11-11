package com.transaction_service.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transaction_service.entity.Transaction;
import com.transaction_service.enums.TransactionStatus;

import jakarta.persistence.LockModeType;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionReference = :ref")
    Optional<Transaction> findByReferenceForUpdate(@Param("ref") String reference);
    
    Optional<Transaction> findByTransactionReference(String reference);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    // ✅ Paginated user transactions (no N+1)
    @Query(value = "SELECT t FROM Transaction t WHERE t.userId = :userId",
           countQuery = "SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // ✅ Optimized query with index hint
    @Query(value = "SELECT t FROM Transaction t " +
                   "WHERE t.userId = :userId " +
                   "AND t.status = :status " +
                   "ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") TransactionStatus status,
        Pageable pageable
    );
    
    // ✅ Batch fetch for account IDs (prevents N+1)
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.sourceAccountId IN :accountIds " +
           "OR t.destinationAccountId IN :accountIds " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountIds(@Param("accountIds") List<Long> accountIds, Pageable pageable);
    
    // ✅ Aggregate query (single query, no iteration)
    @Query(value = "SELECT " +
                   "COUNT(t.id) as totalCount, " +
                   "SUM(t.amount) as totalAmount, " +
                   "SUM(t.feeAmount) as totalFees " +
                   "FROM transactions t " +
                   "WHERE t.user_id = :userId " +
                   "AND t.status = :status " +
                   "AND t.created_at BETWEEN :start AND :end",
           nativeQuery = true)
    TransactionSummaryProjection getUserTransactionSummary(
        @Param("userId") Long userId,
        @Param("status") String status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    // ✅ Bulk status update (single query)
    @Modifying
    @Query("UPDATE Transaction t SET t.status = :newStatus, t.updatedAt = :updatedAt " +
           "WHERE t.status = :oldStatus AND t.createdAt < :cutoffTime")
    int bulkUpdateStatus(
        @Param("oldStatus") TransactionStatus oldStatus,
        @Param("newStatus") TransactionStatus newStatus,
        @Param("updatedAt") LocalDateTime updatedAt,
        @Param("cutoffTime") LocalDateTime cutoffTime
    );
    
    // ✅ Date range with pagination (memory safe)
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :start AND :end " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByDateRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );
    
    // Statistics queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumCompletedAmount(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
