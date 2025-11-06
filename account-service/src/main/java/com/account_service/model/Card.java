package com.account_service.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.account_service.enums.CardStatus;
import com.account_service.enums.CardType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_number", columnList = "card_number"),
    @Index(name = "idx_account_id", columnList = "account_id")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Card {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	    
	    @Column(name = "card_number", unique = true, nullable = false, length = 16)
	    private String cardNumber; // Encrypted
	    
	    @Column(name = "account_id", nullable = false)
	    private Long accountId;
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "card_type", nullable = false, length = 20)
	    private CardType cardType; // DEBIT, CREDIT, VIRTUAL
	    
	    @Column(name = "card_holder_name", nullable = false, length = 100)
	    private String cardHolderName;
	    
	    @Column(name = "expiry_date", nullable = false)
	    private LocalDate expiryDate;
	    
	    @Column(name = "cvv", length = 3)
	    private String cvv; // Encrypted
	    
	    @Column(name = "pin_hash")
	    private String pinHash; // Hashed PIN
	    
	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false, length = 20)
	    private CardStatus status;
	    
	    @Column(name = "is_international_enabled")
	    private Boolean isInternationalEnabled = false;
	    
	    @Column(name = "is_online_enabled")
	    private Boolean isOnlineEnabled = true;
	    
	    @Column(name = "is_contactless_enabled")
	    private Boolean isContactlessEnabled = true;
	    
	    @Column(name = "daily_limit", precision = 19, scale = 4)
	    private BigDecimal dailyLimit;
	    
	    @Column(name = "monthly_limit", precision = 19, scale = 4)
	    private BigDecimal monthlyLimit;
	    
	    @Column(name = "issued_at")
	    private LocalDateTime issuedAt;
	    
	    @Column(name = "activated_at")
	    private LocalDateTime activatedAt;
	    
	    @Column(name = "blocked_at")
	    private LocalDateTime blockedAt;
	    
	    @Column(name = "block_reason")
	    private String blockReason;
	    
	    @CreationTimestamp
	    @Column(name = "created_at", nullable = false, updatable = false)
	    private LocalDateTime createdAt;
	    
	    @UpdateTimestamp
	    @Column(name = "updated_at")
	    private LocalDateTime updatedAt;
}
