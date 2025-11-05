package com.user_service.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.enums.AuditAction;
import com.user_service.model.AuditLog;
import com.user_service.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

	private final AuditService auditService;

	/**
	 * Get current user's audit logs
	 */
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<Page<AuditLog>>> getMyAuditLogs(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		Long userId = 1L; // Extract from JWT
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<AuditLog> logs = auditService.getAuditLogsForUser(userId, pageable);

		return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved successfully"));
	}

	/**
	 * Get audit logs for specific user (Admin only)
	 */
	@GetMapping("/user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<AuditLog>>> getUserAuditLogs(@PathVariable Long userId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<AuditLog> logs = auditService.getAuditLogsForUser(userId, pageable);

		return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved successfully"));
	}

	/**
	 * Get audit logs by action (Admin only)
	 */
	@GetMapping("/action/{action}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogsByAction(@PathVariable AuditAction action,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<AuditLog> logs = auditService.getAuditLogsByAction(action, pageable);

		return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved successfully"));
	}

	/**
	 * Get audit logs by date range (Admin only)
	 */
	@GetMapping("/range")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogsByDateRange(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<AuditLog> logs = auditService.getAuditLogsByDateRange(startDate, endDate, pageable);

		return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved successfully"));
	}
}