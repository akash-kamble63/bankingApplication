package com.notification.DTOs;

import java.time.LocalDateTime;
import java.util.Map;

import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationPriority;
import com.notification.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
	@NotNull(message = "User ID is required")
	private Long userId;

	private String referenceId;

	@NotNull(message = "Notification type is required")
	private NotificationType type;

	@NotNull(message = "Channel is required")
	private NotificationChannel channel;

	private NotificationPriority priority = NotificationPriority.NORMAL;

	@NotBlank(message = "Subject is required")
	private String subject;

	@NotBlank(message = "Content is required")
	private String content;

	private String templateId;

	private Map<String, Object> templateData;

	private String recipientEmail;

	private String recipientPhone;

	private String deviceToken;

	private LocalDateTime scheduledAt;

	private LocalDateTime expiresAt;

	private Map<String, Object> metadata;

	private String correlationId;
}
