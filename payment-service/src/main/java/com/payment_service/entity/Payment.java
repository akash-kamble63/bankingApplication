package com.payment_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.payment_service.enums.PaymentMethod;
import com.payment_service.enums.PaymentStatus;
import com.payment_service.enums.PaymentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_ref", columnList = "payment_reference", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_payment_method", columnList = "payment_method"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_composite", columnList = "user_id, status, created_at DESC"),
        @Index(name = "idx_external_ref", columnList = "external_transaction_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_reference", unique = true, nullable = false, length = 50)
    private String paymentReference; // PAY-XXX-XXX
    
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "account_id")
    private Long accountId; // Source account (optional)
    
    @Column(name = "merchant_id")
    private Long merchantId; // For merchant payments
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "fee_amount", precision = 19, scale = 4)
    private BigDecimal feeAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount; // amount + tax + fee
    
    @Column(length = 3, nullable = false)
    private String currency = "INR";
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;
    
    @Column(length = 500)
    private String description;
    
    // Card payment fields (PCI-DSS compliant - tokenized)
    @Column(name = "card_token", length = 100)
    private String cardToken; // Tokenized card (NOT raw card number)
    
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;
    
    @Column(name = "card_brand", length = 20)
    private String cardBrand; // VISA, MASTERCARD, AMEX
    
    // UPI payment fields
    @Column(name = "upi_id", length = 100)
    private String upiId; // user@bank
    
    @Column(name = "upi_transaction_id", length = 50)
    private String upiTransactionId;
    
    // Bill payment fields
    @Column(name = "biller_id", length = 50)
    private String billerId;
    
    @Column(name = "bill_number", length = 100)
    private String billNumber;
    
    // External gateway fields
    @Column(name = "gateway_name", length = 50)
    private String gatewayName; // RAZORPAY, STRIPE, PAYTM
    
    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;
    
    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;
    
    // Processing fields
    @Column(name = "saga_id", length = 36)
    private String sagaId;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @Column(name = "fraud_score", precision = 5, scale = 2)
    private BigDecimal fraudScore;
    
    @Column(name = "fraud_status", length = 20)
    private String fraudStatus;
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    @Column(name = "gateway_error_code", length = 50)
    private String gatewayErrorCode;
    
    @Column(name = "gateway_error_message", length = 500)
    private String gatewayErrorMessage;
    
    // Recipient details (for P2P)
    @Column(name = "recipient_id")
    private Long recipientId;
    
    @Column(name = "recipient_account_id")
    private Long recipientAccountId;
    
    @Column(name = "recipient_name", length = 100)
    private String recipientName;
    
    // Refund tracking
    @Column(name = "refunded_amount", precision = 19, scale = 4)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    @Column(name = "refund_reference", length = 50)
    private String refundReference;
    
    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // For pending payments
    
    @Version
    private Long version; // Optimistic locking
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // Additional flexible data
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "device_fingerprint", length = 100)
    private String deviceFingerprint;
}
