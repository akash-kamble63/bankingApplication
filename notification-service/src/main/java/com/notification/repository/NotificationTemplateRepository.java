package com.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.notification.entity.NotificationTemplate;
import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationType;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);
    
    Optional<NotificationTemplate> findByTypeAndChannel(
        NotificationType type, NotificationChannel channel
    );
    
    boolean existsByTemplateCode(String templateCode);
}
