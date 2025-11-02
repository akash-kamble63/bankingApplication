package com.user_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Column(length = 10)
    private String gender;
    
    @Column(length = 255)
    private String address;
    
    @Column(length = 100)
    private String occupation;
    
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;
    
    @Column(length = 50)
    private String nationality;
}