package com.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.user_service.enums.Status;
import com.user_service.model.Users;

public interface UserRepository extends JpaRepository<Users, Long> {

	Optional<Users> findByUsername(String username);

	Optional<Users> findByEmail(String username);

	List<Users> findByStatus(Status status);

	Optional<Users> findByAuthId(String authId);

	boolean existsByEmail(String email);

	boolean existsByAuthId(String authId);

	@Query("SELECT u FROM User u WHERE u.status = :status AND u.emailVerifiedAt IS NULL")
	List<Users> findUnverifiedUsersByStatus(@Param("status") Status status);
}
