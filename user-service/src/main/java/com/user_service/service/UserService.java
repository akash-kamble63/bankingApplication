package com.user_service.service;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.UserResponse;

public interface UserService {
	/**
     * Register a new user
     * @param request User registration data
     * @return ApiResponse with user details
     */
    ApiResponse<UserResponse> createUser(CreateUserRequest request);
    
    /**
     * Get user by email
     * @param email User email
     * @return ApiResponse with user details
     */
    ApiResponse<UserResponse> getUserByEmail(String email);
    
    /**
     * Resend verification email
     * @param email User email
     * @return ApiResponse
     */
    ApiResponse<Void> resendVerificationEmail(String email);
}
