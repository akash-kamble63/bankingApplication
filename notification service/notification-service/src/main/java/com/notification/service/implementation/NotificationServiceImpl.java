package com.notification.service.implementation;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.DTOs.NotificationRequest;
import com.notification.DTOs.NotificationResponse;
import com.notification.DTOs.TemplateNotificationRequest;
import com.notification.entity.Notification;
import com.notification.entity.NotificationPreference;
import com.notification.entity.NotificationTemplate;
import com.notification.enums.NotificationStatus;
import com.notification.exception.NotificationException;
import com.notification.repository.NotificationRepository;
import com.notification.repository.NotificationTemplateRepository;
import com.notification.service.NotificationChannelService;
import com.notification.service.NotificationPreferenceService;
import com.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

	private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceService preferenceService;
    private final NotificationChannelService channelService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Sending notification: type={}, channel={}, userId={}", 
            request.getType(), request.getChannel(), request.getUserId());
        
        // Check user preferences
        if (!preferenceService.shouldSendNotification(
                request.getUserId(), 
                request.getType().name(), 
                request.getChannel().name())) {
            log.info("Notification blocked by user preferences");
            throw new NotificationException("Notification blocked by user preferences");
        }
        
        // Check quiet hours
        if (isQuietHours(request.getUserId())) {
            log.info("Scheduling notification due to quiet hours");
            return scheduleNotification(request);
        }
        
        // Create notification entity
        Notification notification = buildNotification(request);
        notification = notificationRepository.save(notification);
        
        // Send via appropriate channel (async)
        sendViaChannel(notification);
        
        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse sendTemplateNotification(TemplateNotificationRequest request) {
        log.info("Sending template notification: templateCode={}, userId={}", 
            request.getTemplateCode(), request.getUserId());
        
        // Load template
        NotificationTemplate template = templateRepository
            .findByTemplateCode(request.getTemplateCode())
            .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (!template.getActive()) {
            throw new NotificationException("Template is inactive");
        }
        
        // Replace template variables
        String content = replaceTemplateVariables(
            template.getContentTemplate(), 
            request.getVariables()
        );
        
        // Create notification request
        NotificationRequest notificationRequest = NotificationRequest.builder()
            .userId(request.getUserId())
            .referenceId(request.getReferenceId())
            .type(template.getType())
            .channel(template.getChannel())
            .subject(template.getSubject())
            .content(content)
            .templateId(template.getTemplateCode())
            .correlationId(request.getCorrelationId())
            .build();
        
        return sendNotification(notificationRequest);
    }

    @Override
    @Transactional
    public NotificationResponse scheduleNotification(NotificationRequest request) {
        log.info("Scheduling notification for userId={} at {}", 
            request.getUserId(), request.getScheduledAt());
        
        Notification notification = buildNotification(request);
        notification.setStatus(NotificationStatus.SCHEDULED);
        notification.setScheduledAt(request.getScheduledAt() != null ? 
            request.getScheduledAt() : LocalDateTime.now().plusMinutes(5));
        
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationException("Unauthorized access");
        }
        
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadNotifications(userId);
    }

    @Override
    @Transactional
    public void cancelNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationException("Unauthorized access");
        }
        
        if (notification.getStatus() == NotificationStatus.SCHEDULED) {
            notification.setStatus(NotificationStatus.CANCELLED);
            notificationRepository.save(notification);
        }
    }

    // Helper methods
    private Notification buildNotification(NotificationRequest request) {
        try {
            String metadataJson = request.getMetadata() != null ? 
                objectMapper.writeValueAsString(request.getMetadata()) : null;
            
            String templateDataJson = request.getTemplateData() != null ?
                objectMapper.writeValueAsString(request.getTemplateData()) : null;
            
            return Notification.builder()
                .userId(request.getUserId())
                .referenceId(request.getReferenceId())
                .type(request.getType())
                .channel(request.getChannel())
                .priority(request.getPriority())
                .status(NotificationStatus.PENDING)
                .subject(request.getSubject())
                .content(request.getContent())
                .templateId(request.getTemplateId())
                .templateData(templateDataJson)
                .recipientEmail(request.getRecipientEmail())
                .recipientPhone(request.getRecipientPhone())
                .deviceToken(request.getDeviceToken())
                .scheduledAt(request.getScheduledAt())
                .expiresAt(request.getExpiresAt())
                .metadata(metadataJson)
                .correlationId(request.getCorrelationId() != null ? 
                    request.getCorrelationId() : UUID.randomUUID().toString())
                .retryCount(0)
                .maxRetries(3)
                .build();
        } catch (Exception e) {
            throw new NotificationException("Failed to build notification", e);
        }
    }

    private void sendViaChannel(Notification notification) {
        try {
            switch (notification.getChannel()) {
                case EMAIL -> channelService.sendEmail(notification);
                case SMS -> channelService.sendSms(notification);
                case PUSH -> channelService.sendPushNotification(notification);
                case IN_APP -> channelService.sendInAppNotification(notification);
                default -> throw new NotificationException("Unsupported channel");
            }
        } catch (Exception e) {
            log.error("Failed to send notification via {}: {}", 
                notification.getChannel(), e.getMessage());
            handleSendFailure(notification, e);
        }
    }

    private void handleSendFailure(Notification notification, Exception e) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setErrorMessage(e.getMessage());
        notificationRepository.save(notification);
    }

    private boolean isQuietHours(Long userId) {
        NotificationPreference prefs = preferenceService.getUserPreferences(userId);
        
        if (!prefs.getQuietHoursEnabled()) {
            return false;
        }
        
        // Check if current time is in quiet hours range
        // Simplified - implement proper time range check
        return false;
    }

    private String replaceTemplateVariables(String template, Map<String, Object> variables) {
        String result = template;
        
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        
        return result;
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .userId(n.getUserId())
            .referenceId(n.getReferenceId())
            .type(n.getType())
            .channel(n.getChannel())
            .priority(n.getPriority())
            .status(n.getStatus())
            .subject(n.getSubject())
            .content(n.getContent())
            .sentAt(n.getSentAt())
            .deliveredAt(n.getDeliveredAt())
            .readAt(n.getReadAt())
            .createdAt(n.getCreatedAt())
            .errorMessage(n.getErrorMessage())
            .build();
    }
}
