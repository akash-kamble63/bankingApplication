package com.transaction_service.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.transaction_service.enums.NotificationStatus;
import com.transaction_service.enums.NotificationType;

@Entity
@Table(name = "transaction_notifications", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionNotification {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    
    @Column(name = "transaction_reference", length = 50)
    private String transactionReference;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationType notificationType; // EMAIL, SMS, PUSH
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;
    
    @Column(name = "recipient", length = 200)
    private String recipient; // Email or phone
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
