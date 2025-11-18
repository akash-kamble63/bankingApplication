package com.card_service.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.card_service.enums.CardNetwork;
import com.card_service.enums.CardStatus;
import com.card_service.enums.CardType;
import com.card_service.enums.CardVariant;
import com.card_service.enums.DeliveryStatus;

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
    name = "cards",
    indexes = {
        @Index(name = "idx_card_number", columnList = "card_number_hash", unique = true),
        @Index(name = "idx_card_token", columnList = "card_token", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_account_id", columnList = "account_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_card_type", columnList = "card_type"),
        @Index(name = "idx_composite", columnList = "user_id, status, created_at DESC")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_reference", unique = true, nullable = false, length = 50)
    private String cardReference; // CARD-XXX-XXX
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "card_number_encrypted", columnDefinition = "TEXT", nullable = false)
    private String cardNumberEncrypted; // AES-256 encrypted
    
    @Column(name = "card_number_hash", unique = true, nullable = false, length = 64)
    private String cardNumberHash; // SHA-256 hash for lookup
    
    @Column(name = "card_token", unique = true, nullable = false, length = 100)
    private String cardToken; // PCI-DSS compliant token for payments
    
    @Column(name = "card_holder_name", nullable = false, length = 100)
    private String cardHolderName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType; // DEBIT, CREDIT
    
    @Enumerated(EnumType.STRING)
    @Column(name = "card_network", nullable = false, length = 20)
    private CardNetwork cardNetwork; // VISA, MASTERCARD, RUPAY, AMEX
    
    @Enumerated(EnumType.STRING)
    @Column(name = "card_variant", length = 30)
    private CardVariant cardVariant; // CLASSIC, GOLD, PLATINUM, SIGNATURE
    
    @Column(name = "is_virtual")
    private Boolean isVirtual = false;
    
    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth; // 1-12
    
    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear; // 2024
    
    @Column(name = "cvv_encrypted", length = 500)
    private String cvvEncrypted; // AES-256 encrypted
    
    @Column(name = "pin_hash", length = 128)
    private String pinHash; // BCrypt hashed PIN
    
    @Column(name = "pin_set")
    private Boolean pinSet = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;
    
    // Limits
    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;
    
    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit;
    
    @Column(name = "per_transaction_limit", precision = 19, scale = 4)
    private BigDecimal perTransactionLimit;
    
    @Column(name = "daily_withdrawal_limit", precision = 19, scale = 4)
    private BigDecimal dailyWithdrawalLimit;
    
    // Usage tracking (reset daily/monthly)
    @Column(name = "daily_spent", precision = 19, scale = 4)
    private BigDecimal dailySpent = BigDecimal.ZERO;
    
    @Column(name = "monthly_spent", precision = 19, scale = 4)
    private BigDecimal monthlySpent = BigDecimal.ZERO;
    
    @Column(name = "daily_withdrawals", precision = 19, scale = 4)
    private BigDecimal dailyWithdrawals = BigDecimal.ZERO;
    
    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;
    
    // Features
    @Column(name = "contactless_enabled")
    private Boolean contactlessEnabled = true;
    
    @Column(name = "online_transactions_enabled")
    private Boolean onlineTransactionsEnabled = true;
    
    @Column(name = "international_transactions_enabled")
    private Boolean internationalTransactionsEnabled = false;
    
    @Column(name = "atm_enabled")
    private Boolean atmEnabled = true;
    
    @Column(name = "pos_enabled")
    private Boolean posEnabled = true;
    
    @Column(name = "three_d_secure_enabled")
    private Boolean threeDSecureEnabled = true;
    
    // Rewards & Benefits
    @Column(name = "reward_points", precision = 19, scale = 2)
    private BigDecimal rewardPoints = BigDecimal.ZERO;
    
    @Column(name = "cashback_percentage", precision = 5, scale = 2)
    private BigDecimal cashbackPercentage;
    
    @Column(name = "annual_fee", precision = 19, scale = 4)
    private BigDecimal annualFee;
    
    @Column(name = "fee_waiver_amount", precision = 19, scale = 4)
    private BigDecimal feeWaiverAmount; // Spend X to waive fee
    
    // Billing (Credit Cards)
    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit;
    
    @Column(name = "available_credit", precision = 19, scale = 4)
    private BigDecimal availableCredit;
    
    @Column(name = "outstanding_balance", precision = 19, scale = 4)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;
    
    @Column(name = "billing_cycle_day")
    private Integer billingCycleDay; // Day of month (1-28)
    
    @Column(name = "payment_due_day")
    private Integer paymentDueDay;
    
    @Column(name = "minimum_payment_percentage", precision = 5, scale = 2)
    private BigDecimal minimumPaymentPercentage;
    
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate; // Annual percentage rate
    
    // Tracking
    @Column(name = "activation_code", length = 6)
    private String activationCode;
    
    @Column(name = "activation_expiry")
    private LocalDateTime activationExpiry;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "last_used_location", length = 200)
    private String lastUsedLocation;
    
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
    
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
    
    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;
    
    @Column(name = "block_reason", length = 500)
    private String blockReason;
    
    @Column(name = "replaced_by_card_id")
    private Long replacedByCardId;
    
    @Column(name = "replaces_card_id")
    private Long replacesCardId;
    
    // Delivery tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 30)
    private DeliveryStatus deliveryStatus;
    
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;
    
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    // Metadata
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version; // Optimistic locking
}
