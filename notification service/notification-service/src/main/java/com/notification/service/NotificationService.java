package com.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.notification.DTOs.NotificationRequest;
import com.notification.DTOs.NotificationResponse;
import com.notification.DTOs.TemplateNotificationRequest;

public interface NotificationService {
	NotificationResponse sendNotification(NotificationRequest request);

	NotificationResponse sendTemplateNotification(TemplateNotificationRequest request);

	NotificationResponse scheduleNotification(NotificationRequest request);

	Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

	NotificationResponse getNotificationById(Long id);

	void markAsRead(Long notificationId, Long userId);

	void markAllAsRead(Long userId);

	Long getUnreadCount(Long userId);

	void cancelNotification(Long notificationId, Long userId);
}
