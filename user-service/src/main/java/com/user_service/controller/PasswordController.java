package com.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.ChangePasswordRequest;
import com.user_service.dto.ForgotPasswordRequest;
import com.user_service.dto.ResetPasswordRequest;
import com.user_service.service.PasswordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequestMapping("/api/users/password")
@RequiredArgsConstructor
@Validated
public class PasswordController {
	
private final PasswordService passwordService;
    
    /**
     * Change password (requires authentication)
     */
    @PostMapping("/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        String email = jwt.getClaimAsString("email");
        log.info("Password change request from user: {}", email);
        
        ApiResponse<Void> response = passwordService.changePassword(email, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Forgot password (public endpoint)
     */
    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        
        log.info("Forgot password request for email: {}", request.getEmail());
        ApiResponse<Void> response = passwordService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reset password with token (public endpoint)
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        
        log.info("Password reset request with token");
        ApiResponse<Void> response = passwordService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate reset token (public endpoint)
     */
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<Void>> validateToken(
            @RequestParam String token) {
        
        log.info("Token validation request");
        ApiResponse<Void> response = passwordService.validateResetToken(token);
        return ResponseEntity.ok(response);
    }

}
