package com.user_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.user_service.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFilterRequest {
    private String email;
    private String firstName;
    private String lastName;
    private List<UserStatus> statuses;
    private String contactNumber;
    private Boolean emailVerified;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private String nationality;
    private String occupation;
    private String gender;
    
    // Sorting
    private String sortBy = "id";
    private String sortDirection = "ASC";
    
    // Pagination
    private int page = 0;
    private int size = 10;
}
