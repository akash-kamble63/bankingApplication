package com.user_service.model;

import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.user_service.enums.Roles;
import com.user_service.enums.Status;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Users {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String username;
	private String password;

	@Enumerated(EnumType.STRING)
	private Roles role;
	private String email;

	private String contactNo;

	private Status status;
	private String authId;

	private String identificationNumber;

	@CreationTimestamp
	private LocalDate createdOn;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "user_profile_id", referencedColumnName = "profileId")
	private Profile userProfile;

}
