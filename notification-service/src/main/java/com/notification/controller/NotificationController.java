package com.notification.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.notification.DTOs.NotificationPreferenceRequest;
import com.notification.DTOs.NotificationRequest;
import com.notification.DTOs.NotificationResponse;
import com.notification.DTOs.TemplateNotificationRequest;
import com.notification.entity.NotificationPreference;
import com.notification.service.NotificationPreferenceService;
import com.notification.service.NotificationService;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Notification endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {
	private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    @Timed(value = "notification.send", description = "Time to send notification")
    @Operation(summary = "Send notification", description = "Send notification to user")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("Sending notification: type={}, channel={}", request.getType(), request.getChannel());
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/template")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    @Operation(summary = "Send template notification")
    public ResponseEntity<NotificationResponse> sendTemplateNotification(
            @Valid @RequestBody TemplateNotificationRequest request) {
        
        NotificationResponse response = notificationService.sendTemplateNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    @Operation(summary = "Schedule notification")
    public ResponseEntity<NotificationResponse> scheduleNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        NotificationResponse response = notificationService.scheduleNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get user notifications", description = "Get paginated notifications")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            Authentication authentication,
            Pageable pageable) {
        
        Long userId = extractUserId(authentication);
        Page<NotificationResponse> notifications = notificationService
            .getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable Long id) {
        
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Long userId = extractUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        Long userId = extractUserId(authentication);
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel scheduled notification")
    public ResponseEntity<Void> cancelNotification(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        notificationService.cancelNotification(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<NotificationPreference> getPreferences(
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        NotificationPreference preferences = preferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        NotificationPreference updated = preferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Notification Service is healthy");
    }

    private Long extractUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
