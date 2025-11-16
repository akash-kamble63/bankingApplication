package com.notification.service;

import com.notification.DTOs.NotificationPreferenceRequest;
import com.notification.entity.NotificationPreference;

public interface NotificationPreferenceService {
	NotificationPreference getUserPreferences(Long userId);
    NotificationPreference updatePreferences(Long userId, NotificationPreferenceRequest request);
    boolean shouldSendNotification(Long userId, String notificationType, String channel);
}
