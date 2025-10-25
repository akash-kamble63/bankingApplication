package com.user_service.service;

import org.springframework.security.core.Authentication;

import com.user_service.dto.AuthResponse;
import com.user_service.dto.UserLogInRequestDTO;
import com.user_service.dto.UserSignUpRequestDTO;

public interface AuthService {
	AuthResponse registerUser(UserSignUpRequestDTO request) throws Exception;
	AuthResponse LoginUser(UserLogInRequestDTO request) throws Exception;
	Authentication authenticate(String username, String password) throws Exception; 
}