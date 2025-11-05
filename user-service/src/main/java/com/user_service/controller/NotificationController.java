package com.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.NotificationPreferencesRequest;
import com.user_service.model.NotificationPreferences;
import com.user_service.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * Get current user's notification preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferences>> getPreferences(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = 1L; // Extract from JWT
        ApiResponse<NotificationPreferences> response = notificationService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update notification preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferences>> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        Long userId = 1L; // Extract from JWT
        ApiResponse<NotificationPreferences> response = 
            notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
}