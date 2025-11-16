package com.notification.DTOs;

import java.time.LocalDateTime;

import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationPriority;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
	private Long id;
    private Long userId;
    private String referenceId;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationPriority priority;
    private NotificationStatus status;
    private String subject;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private String errorMessage;
}
