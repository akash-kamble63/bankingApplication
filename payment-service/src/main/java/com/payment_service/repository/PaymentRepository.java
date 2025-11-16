package com.payment_service.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payment_service.entity.Payment;
import com.payment_service.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long>,
JpaSpecificationExecutor<Payment>{
	// Find with pessimistic lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentReference = :ref")
    Optional<Payment> findByReferenceForUpdate(@Param("ref") String reference);
    
    Optional<Payment> findByPaymentReference(String reference);
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    
    // Paginated user payments (no N+1)
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = :status " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") PaymentStatus status,
        Pageable pageable
    );
    
    // Merchant payments
    @Query("SELECT p FROM Payment p WHERE p.merchantId = :merchantId " +
           "AND p.status = 'COMPLETED' ORDER BY p.createdAt DESC")
    Page<Payment> findCompletedMerchantPayments(
        @Param("merchantId") Long merchantId,
        Pageable pageable
    );
    
    // Aggregate query for analytics (no loops!)
    @Query(value = "SELECT " +
                   "COUNT(p.id) as total_count, " +
                   "SUM(p.amount) as total_amount, " +
                   "SUM(p.fee_amount) as total_fees, " +
                   "AVG(p.amount) as avg_amount " +
                   "FROM payments p " +
                   "WHERE p.user_id = :userId " +
                   "AND p.status = :status " +
                   "AND p.created_at BETWEEN :start AND :end",
           nativeQuery = true)
    PaymentSummaryProjection getUserPaymentSummary(
        @Param("userId") Long userId,
        @Param("status") String status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    // Find expired pending payments (for cleanup job)
    @Query("SELECT p FROM Payment p WHERE p.status IN ('INITIATED', 'PENDING_AUTHORIZATION') " +
           "AND p.expiresAt < :now")
    List<Payment> findExpiredPendingPayments(@Param("now") LocalDateTime now);
    
    // Statistics queries
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") PaymentStatus status);
    
    @Query("SELECT p.paymentMethod as method, COUNT(p) as count, SUM(p.amount) as total " +
           "FROM Payment p WHERE p.status = 'COMPLETED' " +
           "AND p.createdAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentMethod")
    List<PaymentMethodStatistics> getPaymentMethodStatistics(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "AND p.createdAt BETWEEN :start AND :end")
    BigDecimal sumCompletedAmount(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    // Fraud detection queries
    @Query("SELECT COUNT(p) FROM Payment p " +
           "WHERE p.userId = :userId " +
           "AND p.status IN ('COMPLETED', 'PROCESSING') " +
           "AND p.createdAt > :since")
    long countRecentPaymentsByUser(
        @Param("userId") Long userId,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT SUM(p.amount) FROM Payment p " +
           "WHERE p.userId = :userId " +
           "AND p.status = 'COMPLETED' " +
           "AND p.createdAt > :since")
    BigDecimal sumRecentPaymentAmountByUser(
        @Param("userId") Long userId,
        @Param("since") LocalDateTime since
    );
}
