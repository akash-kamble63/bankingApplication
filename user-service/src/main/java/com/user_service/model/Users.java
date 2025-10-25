package com.user_service.model;

import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.user_service.enums.Roles;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "Users")
@Data
public class Users {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private Long id;
	private String username;
	private String password;
	
	@Enumerated(EnumType.STRING)
	private Roles role;
	private String email;
	
	@CreationTimestamp
	private LocalDate localDate;
	
	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name="user_profile_id", referencedColumnName = "profileId")
	private Profile userProfile;
	
	
}
