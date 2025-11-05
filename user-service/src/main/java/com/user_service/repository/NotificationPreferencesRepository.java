package com.user_service.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.user_service.model.NotificationPreferences;

public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {
    Optional<NotificationPreferences> findByUserId(Long userId);
}