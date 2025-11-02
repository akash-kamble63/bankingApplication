package com.user_service.dto;

import java.time.LocalDateTime;

import com.user_service.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
	private Long userId;
    private String username;
    private String email;
    private String contactNumber;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private Boolean emailVerified;
    private String identificationNumber;
    private LocalDateTime createdAt;

}
