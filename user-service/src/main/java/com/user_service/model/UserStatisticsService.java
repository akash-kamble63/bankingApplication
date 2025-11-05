package com.user_service.model;

import java.util.Map;

import com.user_service.dto.ApiResponse;
import com.user_service.model.UserStatistics;

public interface UserStatisticsService {
	UserStatistics getOrCreateStatistics(Long userId);
	void recordLogin(Long userId);
	void recordFailedLogin(Long userId);
	void recordPasswordChange(Long userId);
	void recordProfileUpdate(Long userId);
	ApiResponse<UserStatistics> getUserStatistics(Long userId);
	ApiResponse<Map<String, Object>> getGlobalStatistics();
	
}
