package com.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.user_service.enums.UserStatus;
import com.user_service.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String username);

	List<User> findByStatus(UserStatus status);

	Optional<User> findByAuthId(String authId);

	boolean existsByEmail(String email);

	boolean existsByAuthId(String authId);

	@Query("SELECT u FROM User u WHERE u.status = :status AND u.emailVerifiedAt IS NULL")
	List<User> findUnverifiedUsersByStatus(@Param("status") UserStatus status);
}
