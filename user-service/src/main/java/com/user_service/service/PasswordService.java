package com.user_service.service;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.ChangePasswordRequest;
import com.user_service.dto.ForgotPasswordRequest;
import com.user_service.dto.ResetPasswordRequest;

public interface PasswordService {
	ApiResponse<Void> changePassword(String email, ChangePasswordRequest request);

	ApiResponse<Void> forgotPassword(ForgotPasswordRequest request);

	ApiResponse<Void> resetPassword(ResetPasswordRequest request);

	ApiResponse<Void> validateResetToken(String token);
}
