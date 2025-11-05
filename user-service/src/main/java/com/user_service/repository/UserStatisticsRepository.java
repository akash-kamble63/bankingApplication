package com.user_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.user_service.model.UserStatistics;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

	Optional<UserStatistics> findByUserId(Long userId);

	@Query("SELECT COUNT(u) FROM UserStatistics u WHERE u.lastLoginAt > :since")
	long countActiveUsers(@Param("since") LocalDateTime since);

	@Query("SELECT AVG(u.totalLogins) FROM UserStatistics u")
	Double averageLogins();

	@Query("SELECT SUM(u.totalLogins) FROM UserStatistics u")
	Long totalLogins();
}
