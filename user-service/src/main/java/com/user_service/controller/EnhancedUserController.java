package com.user_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.UserFilterRequest;
import com.user_service.dto.UserResponse;
import com.user_service.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class EnhancedUserController {

	private final UserService userService;

	/**
	 * Advanced user search with filtering
	 */
	@PostMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
			@Valid @RequestBody UserFilterRequest filterRequest) {

		log.info("Searching users with filters");
		ApiResponse<Page<UserResponse>> response = userService.searchUsers(filterRequest);
		return ResponseEntity.ok(response);
	}
}