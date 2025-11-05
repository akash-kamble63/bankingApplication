package com.user_service.service;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.NotificationPreferencesRequest;
import com.user_service.model.NotificationPreferences;

public interface NotificationService {
	NotificationPreferences getOrCreatePreferences(Long userId);
	ApiResponse<NotificationPreferences> getPreferences(Long userId);
	ApiResponse<NotificationPreferences> updatePreferences(
            Long userId, NotificationPreferencesRequest request);
	boolean shouldNotify(Long userId, String eventType);
	
}
