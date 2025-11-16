package com.notification.service.implementation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notification.DTOs.NotificationPreferenceRequest;
import com.notification.entity.NotificationPreference;
import com.notification.repository.NotificationPreferenceRepository;
import com.notification.service.NotificationPreferenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {
    
    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreference getUserPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultPreferences(userId));
    }

    @Override
    @Transactional
    public NotificationPreference updatePreferences(Long userId, 
                                                   NotificationPreferenceRequest request) {
        NotificationPreference preferences = preferenceRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultPreferences(userId));
        
        // Update fields
        if (request.getEmailEnabled() != null) {
            preferences.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getSmsEnabled() != null) {
            preferences.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getPushEnabled() != null) {
            preferences.setPushEnabled(request.getPushEnabled());
        }
        if (request.getInAppEnabled() != null) {
            preferences.setInAppEnabled(request.getInAppEnabled());
        }
        if (request.getTransactionAlerts() != null) {
            preferences.setTransactionAlerts(request.getTransactionAlerts());
        }
        if (request.getPaymentAlerts() != null) {
            preferences.setPaymentAlerts(request.getPaymentAlerts());
        }
        if (request.getSecurityAlerts() != null) {
            preferences.setSecurityAlerts(request.getSecurityAlerts());
        }
        if (request.getMarketingAlerts() != null) {
            preferences.setMarketingAlerts(request.getMarketingAlerts());
        }
        if (request.getPromotionalAlerts() != null) {
            preferences.setPromotionalAlerts(request.getPromotionalAlerts());
        }
        if (request.getQuietHoursEnabled() != null) {
            preferences.setQuietHoursEnabled(request.getQuietHoursEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            preferences.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            preferences.setQuietHoursEnd(request.getQuietHoursEnd());
        }
        
        return preferenceRepository.save(preferences);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendNotification(Long userId, String notificationType, String channel) {
        NotificationPreference prefs = getUserPreferences(userId);
        
        // Check channel preference
        boolean channelEnabled = switch (channel.toUpperCase()) {
            case "EMAIL" -> prefs.getEmailEnabled();
            case "SMS" -> prefs.getSmsEnabled();
            case "PUSH" -> prefs.getPushEnabled();
            case "IN_APP" -> prefs.getInAppEnabled();
            default -> true;
        };
        
        if (!channelEnabled) {
            return false;
        }
        
        // Check notification type preference
        boolean typeEnabled = switch (notificationType) {
            case "TRANSACTION_COMPLETED", "TRANSACTION_FAILED", 
                 "ACCOUNT_CREDITED", "ACCOUNT_DEBITED" -> prefs.getTransactionAlerts();
            case "PAYMENT_SUCCESS", "PAYMENT_FAILED", 
                 "CARD_PAYMENT", "UPI_PAYMENT" -> prefs.getPaymentAlerts();
            case "SECURITY_ALERT", "LOGIN_ALERT", 
                 "PASSWORD_CHANGED" -> prefs.getSecurityAlerts();
            case "MARKETING" -> prefs.getMarketingAlerts();
            case "PROMOTIONAL" -> prefs.getPromotionalAlerts();
            default -> true; // Allow by default
        };
        
        return typeEnabled;
    }

    private NotificationPreference createDefaultPreferences(Long userId) {
        NotificationPreference preferences = NotificationPreference.builder()
            .userId(userId)
            .emailEnabled(true)
            .smsEnabled(true)
            .pushEnabled(true)
            .inAppEnabled(true)
            .transactionAlerts(true)
            .paymentAlerts(true)
            .securityAlerts(true)
            .marketingAlerts(false)
            .promotionalAlerts(false)
            .quietHoursEnabled(false)
            .build();
        
        return preferenceRepository.save(preferences);
    }
}
