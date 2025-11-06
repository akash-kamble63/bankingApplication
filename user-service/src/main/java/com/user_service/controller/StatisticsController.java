package com.user_service.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.model.UserStatistics;
import com.user_service.service.UserStatisticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

	private final UserStatisticsService statisticsService;

	private Long extractUserId(Jwt jwt) {
		String sub = jwt.getClaimAsString("sub");
		return Long.parseLong(jwt.getClaimAsString("user_id"));
	}

	/**
	 * Get current user statistics
	 */
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserStatistics>> getMyStatistics(@AuthenticationPrincipal Jwt jwt) {
		String email = jwt.getClaimAsString("email");
		// Extract user ID from JWT or get from database
		Long userId = extractUserId(jwt); // Replace with actual extraction

		ApiResponse<UserStatistics> response = statisticsService.getUserStatistics(userId);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get user statistics by ID (Admin only)
	 */
	@GetMapping("/user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserStatistics>> getUserStatistics(@PathVariable Long userId) {
		ApiResponse<UserStatistics> response = statisticsService.getUserStatistics(userId);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get global statistics (Admin only)
	 */
	@GetMapping("/global")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getGlobalStatistics() {
		ApiResponse<Map<String, Object>> response = statisticsService.getGlobalStatistics();
		return ResponseEntity.ok(response);
	}
}
