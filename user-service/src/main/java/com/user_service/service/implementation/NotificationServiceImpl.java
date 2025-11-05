package com.user_service.service.implementation;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.NotificationPreferencesRequest;
import com.user_service.model.NotificationPreferences;
import com.user_service.repository.NotificationPreferencesRepository;
import com.user_service.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    
    private final NotificationPreferencesRepository preferencesRepository;
    
    /**
     * Get or create default preferences for user
     */
    @Transactional
    public NotificationPreferences getOrCreatePreferences(Long userId) {
        return preferencesRepository.findByUserId(userId)
            .orElseGet(() -> {
                NotificationPreferences prefs = NotificationPreferences.builder()
                    .userId(userId)
                    .emailEnabled(true)
                    .smsEnabled(false)
                    .pushEnabled(true)
                    .emailOnLogin(true)
                    .emailOnPasswordChange(true)
                    .emailOnProfileUpdate(false)
                    .emailOnAccountActivity(true)
                    .emailMarketing(false)
                    .emailSecurityAlerts(true)
                    .build();
                return preferencesRepository.save(prefs);
            });
    }
    
    /**
     * Get notification preferences
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "notifications", key = "'user:' + #userId")
    public ApiResponse<NotificationPreferences> getPreferences(Long userId) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        return ApiResponse.success(prefs, "Preferences retrieved successfully");
    }
    
    /**
     * Update notification preferences
     */
    @Transactional
    @CacheEvict(value = "notifications", key = "'user:' + #userId")
    public ApiResponse<NotificationPreferences> updatePreferences(
            Long userId, NotificationPreferencesRequest request) {
        
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        
        if (request.getEmailEnabled() != null) {
            prefs.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getSmsEnabled() != null) {
            prefs.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getPushEnabled() != null) {
            prefs.setPushEnabled(request.getPushEnabled());
        }
        if (request.getEmailOnLogin() != null) {
            prefs.setEmailOnLogin(request.getEmailOnLogin());
        }
        if (request.getEmailOnPasswordChange() != null) {
            prefs.setEmailOnPasswordChange(request.getEmailOnPasswordChange());
        }
        if (request.getEmailOnProfileUpdate() != null) {
            prefs.setEmailOnProfileUpdate(request.getEmailOnProfileUpdate());
        }
        if (request.getEmailOnAccountActivity() != null) {
            prefs.setEmailOnAccountActivity(request.getEmailOnAccountActivity());
        }
        if (request.getEmailMarketing() != null) {
            prefs.setEmailMarketing(request.getEmailMarketing());
        }
        if (request.getEmailSecurityAlerts() != null) {
            prefs.setEmailSecurityAlerts(request.getEmailSecurityAlerts());
        }
        
        NotificationPreferences updated = preferencesRepository.save(prefs);
        log.info("Updated notification preferences for user: {}", userId);
        
        return ApiResponse.success(updated, "Preferences updated successfully");
    }
    
    /**
     * Check if user wants notification for specific event
     */
    public boolean shouldNotify(Long userId, String eventType) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        
        if (!prefs.getEmailEnabled()) {
            return false;
        }
        
        return switch (eventType) {
            case "LOGIN" -> prefs.getEmailOnLogin();
            case "PASSWORD_CHANGE" -> prefs.getEmailOnPasswordChange();
            case "PROFILE_UPDATE" -> prefs.getEmailOnProfileUpdate();
            case "ACCOUNT_ACTIVITY" -> prefs.getEmailOnAccountActivity();
            case "SECURITY_ALERT" -> prefs.getEmailSecurityAlerts();
            default -> false;
        };
    }
}