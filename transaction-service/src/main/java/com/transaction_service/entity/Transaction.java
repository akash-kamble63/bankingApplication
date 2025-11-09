package com.transaction_service.entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.enums.TransactionType;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_ref", columnList = "transaction_reference", unique = true),
    @Index(name = "idx_account_number", columnList = "account_number"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_type", columnList = "transaction_type"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_from_to", columnList = "from_account, to_account")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_reference", unique = true, nullable = false, length = 50)
    private String transactionReference; // TXN-UUID
    
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber; // Main account
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(length = 3, nullable = false)
    private String currency = "INR";
    
    // For transfers
    @Column(name = "from_account", length = 20)
    private String fromAccount;
    
    @Column(name = "to_account", length = 20)
    private String toAccount;
    
    @Column(name = "to_account_holder_name", length = 100)
    private String toAccountHolderName;
    
    // Balance tracking
    @Column(name = "opening_balance", precision = 19, scale = 4)
    private BigDecimal openingBalance;
    
    @Column(name = "closing_balance", precision = 19, scale = 4)
    private BigDecimal closingBalance;
    
    // Transaction details
    @Column(length = 500)
    private String description;
    
    @Column(length = 200)
    private String remarks;
    
    @Column(length = 100)
    private String category; // FOOD, TRANSPORT, BILLS, etc.
    
    // Payment details
    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // UPI, NEFT, RTGS, IMPS, CARD, CASH
    
    @Column(name = "upi_id", length = 100)
    private String upiId;
    
    @Column(name = "bank_reference", length = 100)
    private String bankReference; // UTR/RRN number
    
    // Fee and charges
    @Column(precision = 19, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal tax = BigDecimal.ZERO;
    
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;
    
    // Location
    @Column(name = "merchant_name", length = 200)
    private String merchantName;
    
    @Column(name = "merchant_category", length = 50)
    private String merchantCategory;
    
    @Column(name = "location", length = 200)
    private String location;
    
    // Metadata
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "device_id", length = 100)
    private String deviceId;
    
    @Column(name = "channel", length = 50)
    private String channel; // WEB, MOBILE, ATM, BRANCH
    
    // Failure details
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    // Saga tracking
    @Column(name = "saga_id", length = 36)
    private String sagaId;
    
    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;
    
    @Version
    private Long version;
}
