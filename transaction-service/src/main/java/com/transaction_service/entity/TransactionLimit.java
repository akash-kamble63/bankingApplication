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
import com.transaction_service.enums.LimitType;
import com.transaction_service.enums.TransactionType;

@Entity
@Table(name = "transaction_limits", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionLimit {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	    
	    @Column(name = "account_id")
	    private Long accountId;
	    
	    @Column(name = "user_id", nullable = false)
	    private Long userId;
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "transaction_type", length = 30)
	    private TransactionType transactionType;
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "limit_type", nullable = false, length = 20)
	    private LimitType limitType; // DAILY, WEEKLY, MONTHLY, TRANSACTION
	    
	    @Column(name = "max_amount", precision = 19, scale = 4)
	    private BigDecimal maxAmount;
	    
	    @Column(name = "max_count")
	    private Integer maxCount;
	    
	    @Column(name = "current_amount", precision = 19, scale = 4)
	    private BigDecimal currentAmount = BigDecimal.ZERO;
	    
	    @Column(name = "current_count")
	    private Integer currentCount = 0;
	    
	    @Column(name = "reset_at")
	    private LocalDateTime resetAt;
	    
	    @CreationTimestamp
	    @Column(name = "created_at", nullable = false, updatable = false)
	    private LocalDateTime createdAt;
	    
	    @UpdateTimestamp
	    @Column(name = "updated_at")
	    private LocalDateTime updatedAt;
}
