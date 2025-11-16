package com.notification.entity.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationChannelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {
	 private final NotificationRepository notificationRepository;
	    private final NotificationChannelService channelService;

	    @Scheduled(fixedDelay = 10000) // Every 10 seconds
	    @Transactional
	    public void processScheduledNotifications() {
	        List<Notification> scheduled = notificationRepository
	            .findScheduledNotifications(LocalDateTime.now());
	        
	        if (!scheduled.isEmpty()) {
	            log.info("Processing {} scheduled notifications", scheduled.size());
	            
	            for (Notification notification : scheduled) {
	                try {
	                    notification.setStatus(NotificationStatus.PENDING);
	                    notificationRepository.save(notification);
	                    
	                    sendViaChannel(notification);
	                    
	                } catch (Exception e) {
	                    log.error("Failed to send scheduled notification: {}", 
	                        notification.getId(), e);
	                }
	            }
	        }
	    }

	    @Scheduled(fixedDelay = 30000) // Every 30 seconds
	    @Transactional
	    public void retryFailedNotifications() {
	        List<Notification> pending = notificationRepository
	            .findPendingNotifications(NotificationStatus.FAILED, LocalDateTime.now());
	        
	        if (!pending.isEmpty()) {
	            log.info("Retrying {} failed notifications", pending.size());
	            
	            for (Notification notification : pending) {
	                if (notification.getRetryCount() < notification.getMaxRetries()) {
	                    try {
	                        notification.setStatus(NotificationStatus.PENDING);
	                        notification.setRetryCount(notification.getRetryCount() + 1);
	                        notificationRepository.save(notification);
	                        
	                        sendViaChannel(notification);
	                        
	                    } catch (Exception e) {
	                        log.error("Retry failed for notification: {}", 
	                            notification.getId(), e);
	                    }
	                }
	            }
	        }
	    }

	    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
	    @Transactional
	    public void expireOldNotifications() {
	        List<Notification> expired = notificationRepository
	            .findExpiredNotifications(LocalDateTime.now());
	        
	        if (!expired.isEmpty()) {
	            log.info("Expiring {} old notifications", expired.size());
	            
	            for (Notification notification : expired) {
	                notification.setStatus(NotificationStatus.EXPIRED);
	                notificationRepository.save(notification);
	            }
	        }
	    }

	    @Scheduled(cron = "0 0 4 * * ?") // Daily at 4 AM
	    @Transactional
	    public void cleanupOldNotifications() {
	        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
	        notificationRepository.deleteByStatusAndCreatedAtBefore(
	            NotificationStatus.READ, threshold
	        );
	        log.info("Cleaned up old notifications");
	    }

	    private void sendViaChannel(Notification notification) {
	        switch (notification.getChannel()) {
	            case EMAIL -> channelService.sendEmail(notification);
	            case SMS -> channelService.sendSms(notification);
	            case PUSH -> channelService.sendPushNotification(notification);
	            case IN_APP -> channelService.sendInAppNotification(notification);
	        }
	    }
}
