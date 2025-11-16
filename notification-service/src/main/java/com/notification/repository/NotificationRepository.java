package com.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;

import io.lettuce.core.dynamic.annotation.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(
        Long userId, NotificationStatus status, Pageable pageable
    );
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status " +
           "AND n.retryCount < n.maxRetries " +
           "AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now) " +
           "ORDER BY n.priority DESC, n.createdAt ASC")
    List<Notification> findPendingNotifications(
        @Param("status") NotificationStatus status,
        @Param("now") LocalDateTime now
    );
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'SCHEDULED' " +
           "AND n.scheduledAt <= :now")
    List<Notification> findScheduledNotifications(@Param("now") LocalDateTime now);
    
    @Query("SELECT n FROM Notification n WHERE n.status IN ('PENDING', 'SCHEDULED') " +
           "AND n.expiresAt < :now")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId " +
           "AND n.status = 'SENT' AND n.readAt IS NULL")
    Long countUnreadNotifications(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.readAt = :readAt " +
           "WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("status") NotificationStatus status,
                    @Param("readAt") LocalDateTime readAt);
    
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId " +
           "AND n.status = 'SENT' AND n.readAt IS NULL")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
    
    @Query("SELECT n.channel as channel, COUNT(n) as count FROM Notification n " +
           "WHERE n.userId = :userId AND n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.channel")
    List<ChannelStatistics> getChannelStatistics(
        @Param("userId") Long userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    void deleteByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime before);
}

