package com.transaction_service.entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "recurring_payments", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_next_payment", columnList = "next_payment_date")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecurringPayment {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "recurring_id", unique = true, length = 50)
    private String recurringId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;
    
    @Column(name = "biller_name", nullable = false, length = 200)
    private String billerName;
    
    @Column(name = "bill_category", length = 50)
    private String billCategory; // ELECTRICITY, WATER, INTERNET, etc.
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "day_of_month")
    private Integer dayOfMonth; // 1-31
    
    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;
    
    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
