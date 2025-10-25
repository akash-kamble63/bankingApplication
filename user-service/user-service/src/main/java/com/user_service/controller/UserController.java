package com.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.service.UserService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
public class UserController {
	
	private final UserService userService;
	
	@GetMapping
	public ResponseEntity<?> getUserProfile(){
		
		return ResponseEntity.ok("");
	}
	
	
	
}
