package com.user_service.controller;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.response.Response;
import com.user_service.service.KeycloakService;
import com.user_service.service.UserService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
public class UserController {
	
	private final UserService userService;
	private final KeycloakService keycloakService;
	
	@GetMapping("/test")
	public ResponseEntity<?> getUserProfile(){
		
		return ResponseEntity.ok("controller called");
	}
	
	@PostMapping("/register")
	public ResponseEntity<?> createUser(@RequestBody CreateUserRequest userDTO){
		
		log.info("------------------------> controller called");
		System.out.println("------------------------> controller called");
		log.info("creating user with: {}", userDTO.toString());
		
		return ResponseEntity.ok(userService.createUser(userDTO));
	}
	
	@PostMapping("/resend-verification")
	public ResponseEntity<?> resendVerificationEmail(@RequestParam String email) {
	    log.info("Resending verification email to: {}", email);
	    
	    List<UserRepresentation> users = keycloakService.readUserByEmail(email);
	    if(users.isEmpty()) {
	        return ResponseEntity.status(404)
	            .body(Response.builder()
	                .responseCode("404")
	                .responseMessage("User not found")
	                .build());
	    }
	    
	    keycloakService.sendVerificationEmail(users.get(0).getId());
	    
	    return ResponseEntity.ok(Response.builder()
	        .responseCode("200")
	        .responseMessage("Verification email sent successfully")
	        .build());
	}
	
}
