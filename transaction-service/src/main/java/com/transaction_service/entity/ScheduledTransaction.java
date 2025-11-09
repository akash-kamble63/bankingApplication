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
import com.transaction_service.enums.ScheduleFrequency;
import com.transaction_service.enums.ScheduleStatus;
import com.transaction_service.enums.TransactionType;

@Entity
@Table(name = "scheduled_transactions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_next_execution", columnList = "next_execution_date"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledTransaction {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "schedule_reference", unique = true, length = 50)
    private String scheduleReference;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "from_account", nullable = false, length = 20)
    private String fromAccount;
    
    @Column(name = "to_account", nullable = false, length = 20)
    private String toAccount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(length = 3)
    private String currency = "INR";
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleFrequency frequency; // ONCE, DAILY, WEEKLY, MONTHLY
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;
    
    @Column(name = "last_execution_date")
    private LocalDate lastExecutionDate;
    
    @Column(name = "execution_count")
    private Integer executionCount = 0;
    
    @Column(name = "max_executions")
    private Integer maxExecutions;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
