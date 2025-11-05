package com.user_service.service.implementation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.user_service.dto.ApiResponse;
import com.user_service.model.UserStatistics;
import com.user_service.repository.UserRepository;
import com.user_service.repository.UserStatisticsRepository;
import com.user_service.service.UserStatisticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsServiceImpl implements UserStatisticsService {

	private final UserStatisticsRepository statisticsRepository;
    private final UserRepository userRepository;
    
    /**
     * Get or create statistics for user
     */
    @Transactional
    public UserStatistics getOrCreateStatistics(Long userId) {
        return statisticsRepository.findByUserId(userId)
            .orElseGet(() -> {
                UserStatistics stats = UserStatistics.builder()
                    .userId(userId)
                    .totalLogins(0)
                    .totalPasswordChanges(0)
                    .totalProfileUpdates(0)
                    .totalFailedLogins(0)
                    .accountCreatedAt(LocalDateTime.now())
                    .build();
                return statisticsRepository.save(stats);
            });
    }
    
    /**
     * Record login
     */
    @Transactional
    public void recordLogin(Long userId) {
        UserStatistics stats = getOrCreateStatistics(userId);
        stats.setTotalLogins(stats.getTotalLogins() + 1);
        stats.setLastLoginAt(LocalDateTime.now());
        stats.setLastActivityAt(LocalDateTime.now());
        statisticsRepository.save(stats);
        log.info("Recorded login for user: {}", userId);
    }
    
    /**
     * Record failed login
     */
    @Transactional
    public void recordFailedLogin(Long userId) {
        UserStatistics stats = getOrCreateStatistics(userId);
        stats.setTotalFailedLogins(stats.getTotalFailedLogins() + 1);
        statisticsRepository.save(stats);
        log.info("Recorded failed login for user: {}", userId);
    }
    
    /**
     * Record password change
     */
    @Transactional
    public void recordPasswordChange(Long userId) {
        UserStatistics stats = getOrCreateStatistics(userId);
        stats.setTotalPasswordChanges(stats.getTotalPasswordChanges() + 1);
        stats.setLastActivityAt(LocalDateTime.now());
        statisticsRepository.save(stats);
    }
    
    /**
     * Record profile update
     */
    @Transactional
    public void recordProfileUpdate(Long userId) {
        UserStatistics stats = getOrCreateStatistics(userId);
        stats.setTotalProfileUpdates(stats.getTotalProfileUpdates() + 1);
        stats.setLastActivityAt(LocalDateTime.now());
        statisticsRepository.save(stats);
    }
    
    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "statistics", key = "'user:' + #userId")
    public ApiResponse<UserStatistics> getUserStatistics(Long userId) {
        UserStatistics stats = statisticsRepository.findByUserId(userId)
            .orElse(null);
        return ApiResponse.success(stats, "Statistics retrieved successfully");
    }
    
    /**
     * Get global statistics
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "statistics", key = "'global'")
    public ApiResponse<Map<String, Object>> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalUsers", userRepository.count());
        stats.put("totalLogins", statisticsRepository.totalLogins());
        stats.put("averageLogins", statisticsRepository.averageLogins());
        stats.put("activeUsersLast7Days", 
            statisticsRepository.countActiveUsers(LocalDateTime.now().minusDays(7)));
        stats.put("activeUsersLast30Days", 
            statisticsRepository.countActiveUsers(LocalDateTime.now().minusDays(30)));
        
        return ApiResponse.success(stats, "Global statistics retrieved successfully");
    }
}
