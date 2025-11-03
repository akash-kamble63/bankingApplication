package com.user_service.controller;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.UserResponse;
import com.user_service.enums.UserStatus;
import com.user_service.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
	
private final UserService userService;
    //==========================TEST===========================
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("User service is running");
    }
    
    //==================================CREATE METHDOS=================================
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody CreateUserRequest request) {
        
        log.info("User registration request received for email: {}", request.getEmailId());
        ApiResponse<UserResponse> response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    //===================================READ METHDOS=================================
    
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(
            @PathVariable String email) {
        
        log.info("Fetching user by email: {}", email);
        ApiResponse<UserResponse> response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(@AuthenticationPrincipal Jwt jwt){
    	String email = jwt.getClaimAsString("email");
    	log.info("Fetching the current user:{}",email);
    	ApiResponse<UserResponse> response = userService.getCurrentUser(email);
    	return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(@PathVariable long userId){
    
    	log.info("Fetching the user:{}",userId);
    	ApiResponse<UserResponse> response = userService.getUserById(userId);
    	return ResponseEntity.ok(response);
    }
    
    @GetMapping("/auth/{authId}")
    public ResponseEntity<ApiResponse<?>> getUserByAuthId(@PathVariable String authId){
    	
    	log.info("Fetching the user buy auth id: {}", authId);
    	ApiResponse<UserResponse> response = userService.getUserByAuthId(authId);
    	return ResponseEntity.ok(response);
    	
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
    		@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir){
    	log.info("Fetching all users - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDir);
    	Sort sort = sortDir.equalsIgnoreCase("ASC")
    			?Sort.by(sortBy).ascending()
    			:Sort.by(sortBy).descending();
    	Pageable pageable = PageRequest.of(page, size, sort);
    	ApiResponse<Page<UserResponse>> response = userService.getAllUsers(pageable);
    	
    	return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsersByStatus(
    		@PathVariable UserStatus status,
    		@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir){
    	log.info("Fetching all users - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDir);
    	Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
    	Pageable pageable = PageRequest.of(page, size, sort);
    	ApiResponse<Page<UserResponse>> response = userService.getUsersByStatus(status, pageable);

    	return ResponseEntity.ok(response);

    }
    
    
    //===================================OTHER METHDOS=================================
    
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam String email) {
        
        log.info("Resend verification request for email: {}", email);
        ApiResponse<Void> response = userService.resendVerificationEmail(email);
        return ResponseEntity.ok(response);
    }
	
}
