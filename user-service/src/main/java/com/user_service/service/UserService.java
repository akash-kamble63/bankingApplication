package com.user_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.ChangePasswordRequest;
import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.UpdateProfileRequest;
import com.user_service.dto.UpdateUserRequest;
import com.user_service.dto.UserResponse;
import com.user_service.enums.UserStatus;

public interface UserService {
	/**
	 * Register a new user
	 * 
	 * @param request User registration data
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> createUser(CreateUserRequest request);

	/**
	 * Get user by email
	 * 
	 * @param User email
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> getUserByEmail(String email);

	/**
	 * Resend verification email
	 * 
	 * @param email User email
	 * @return ApiResponse
	 */
	ApiResponse<Void> resendVerificationEmail(String email);

	/**
	 * Get user by authentication ID (Keycloak ID)
	 * 
	 * @param authId Keycloak user ID
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> getUserByAuthId(String authId);

	/**
	 * Get user by ID
	 * 
	 * @param userId User ID
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> getUserById(Long userId);

	/**
	 * Get currently authenticated user Uses JWT token to identify user
	 * 
	 * @param Email from JWT token
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> getCurrentUser(String email);

	// Update
	/**
	 * Update user details by use id
	 * 
	 * @param userId
	 * @param request
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> updateUser(Long userId, UpdateUserRequest request);

	/**
	 * Update current user details by use email
	 * 
	 * @param email
	 * @param request
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> updateCurrentUser(String email, UpdateUserRequest request);

	/**
	 * Update current user profile by use id
	 * 
	 * @param userId
	 * @param request
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> updateUserProfile(Long userId, UpdateProfileRequest request);

	/**
	 * Update current users profile by use email
	 * 
	 * @param email
	 * @param request
	 * @return ApiResponse with user details
	 */
	ApiResponse<UserResponse> updateCurrentUserProfile(String email, UpdateProfileRequest request);

	// Delete
	/**
	 * delete user by userId
	 * 
	 * @param userId
	 * @return
	 */
	ApiResponse<Void> deleteUser(Long userId);

	/**
	 * deactivate user by userId
	 * 
	 * @param userId
	 * @return
	 */
	ApiResponse<Void> deactivateUser(Long userId);

	/**
	 * activate user by userId
	 * 
	 * @param userId
	 * @return
	 */
	ApiResponse<Void> activateUser(Long userId);

	ApiResponse<Page<UserResponse>> getAllUsers(Pageable pageable);
	ApiResponse<Page<UserResponse>> getUsersByStatus(UserStatus status, Pageable pageable);
	
	
//	ApiResponse<Page<ActivityLogResponse>> getUserActivityLogs(Long userId, Pageable pageable);

}
