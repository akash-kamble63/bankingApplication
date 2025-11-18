package com.card_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.card_service.enums.CardTransactionStatus;
import com.card_service.enums.CardTransactionType;

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
    name = "card_transactions",
    indexes = {
        @Index(name = "idx_card_id", columnList = "card_id"),
        @Index(name = "idx_transaction_ref", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_authorization_code", columnList = "authorization_code"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at DESC")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_reference", unique = true, nullable = false, length = 50)
    private String transactionReference;
    
    @Column(name = "card_id", nullable = false)
    private Long cardId;
    
    @Column(name = "card_token", nullable = false, length = 100)
    private String cardToken;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private CardTransactionType transactionType; // PURCHASE, WITHDRAWAL, REFUND, etc.
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(length = 3, nullable = false)
    private String currency;
    
    @Column(name = "merchant_name", length = 200)
    private String merchantName;
    
    @Column(name = "merchant_id", length = 100)
    private String merchantId;
    
    @Column(name = "merchant_category_code", length = 10)
    private String merchantCategoryCode; // MCC code
    
    @Column(name = "merchant_city", length = 100)
    private String merchantCity;
    
    @Column(name = "merchant_country", length = 3)
    private String merchantCountry; // ISO 3166
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardTransactionStatus status;
    
    @Column(name = "authorization_code", length = 20)
    private String authorizationCode;
    
    @Column(name = "rrn", length = 20) // Retrieval Reference Number
    private String rrn;
    
    @Column(name = "terminal_id", length = 20)
    private String terminalId;
    
    @Column(name = "is_international")
    private Boolean isInternational = false;
    
    @Column(name = "is_contactless")
    private Boolean isContactless = false;
    
    @Column(name = "is_online")
    private Boolean isOnline = false;
    
    @Column(name = "three_d_secure_verified")
    private Boolean threeDSecureVerified = false;
    
    @Column(name = "fraud_score", precision = 5, scale = 2)
    private BigDecimal fraudScore;
    
    @Column(name = "decline_reason", length = 500)
    private String declineReason;
    
    @Column(name = "reward_points_earned", precision = 19, scale = 2)
    private BigDecimal rewardPointsEarned;
    
    @Column(name = "cashback_earned", precision = 19, scale = 4)
    private BigDecimal cashbackEarned;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "settled_at")
    private LocalDateTime settledAt;
    
    @Version
    private Long version;
}
