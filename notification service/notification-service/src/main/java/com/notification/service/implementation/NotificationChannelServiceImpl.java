package com.notification.service.implementation;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationChannelService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationChannelServiceImpl implements NotificationChannelService {
	private final NotificationRepository notificationRepository;
	private final JavaMailSender mailSender;
	private final WebClient smsWebClient;
	private final WebClient pushWebClient;

	@Value("${notification.email.from:noreply@banking.com}")
	private String fromEmail;

	@Value("${notification.sms.api-key:dummy-key}")
	private String smsApiKey;

	@Value("${notification.push.api-key:dummy-key}")
	private String pushApiKey;

	@Override
	@Async("notificationExecutor")
	@CircuitBreaker(name = "email-service", fallbackMethod = "emailFallback")
	@Retry(name = "email-service")
	@Transactional
	public void sendEmail(Notification notification) {
		log.info("Sending email notification: id={}, to={}", notification.getId(), notification.getRecipientEmail());

		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(notification.getRecipientEmail());
			message.setSubject(notification.getSubject());
			message.setText(notification.getContent());

			mailSender.send(message);

			// Update notification status
			updateNotificationStatus(notification.getId(), NotificationStatus.SENT);
			log.info("Email sent successfully: id={}", notification.getId());

		} catch (Exception e) {
			log.error("Failed to send email: id={}, error={}", notification.getId(), e.getMessage());
			throw e;
		}
	}

	@Override
	@Async("notificationExecutor")
	@CircuitBreaker(name = "sms-service", fallbackMethod = "smsFallback")
	@Retry(name = "sms-service")
	@Transactional
	public void sendSms(Notification notification) {
		log.info("Sending SMS notification: id={}, to={}", notification.getId(), notification.getRecipientPhone());

		try {
			// Call SMS gateway (Twilio, AWS SNS, etc.)
			Map<String, Object> smsRequest = Map.of("to", notification.getRecipientPhone(), "message",
					notification.getContent(), "apiKey", smsApiKey);

			Map<String, Object> response = smsWebClient.post()
			        .uri("/send")
			        .bodyValue(smsRequest)
			        .retrieve()
			        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
			        .block();

			String messageId = (String) response.get("messageId");

			Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
			updated.setStatus(NotificationStatus.SENT);
			updated.setSentAt(LocalDateTime.now());
			updated.setExternalId(messageId);
			notificationRepository.save(updated);

			log.info("SMS sent successfully: id={}, messageId={}", notification.getId(), messageId);

		} catch (Exception e) {
			log.error("Failed to send SMS: id={}, error={}", notification.getId(), e.getMessage());
			throw e;
		}
	}

	@Override
	@Async("notificationExecutor")
	@CircuitBreaker(name = "push-service", fallbackMethod = "pushFallback")
	@Retry(name = "push-service")
	@Transactional
	public void sendPushNotification(Notification notification) {
		log.info("Sending push notification: id={}, deviceToken={}", 
	            notification.getId(), notification.getDeviceToken());

	    try {
	        Map<String, Object> pushRequest = Map.of(
	                "to", notification.getDeviceToken(),
	                "notification", Map.of(
	                        "title", notification.getSubject(),
	                        "body", notification.getContent()
	                ),
	                "priority", "high"
	        );

	        Map<String, Object> response = pushWebClient.post()
	                .uri("/fcm/send")
	                .bodyValue(pushRequest)
	                .retrieve()
	                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
	                .block();

	        updateNotificationStatus(notification.getId(), NotificationStatus.SENT);
	        log.info("Push notification sent: id={}", notification.getId());

	    } catch (Exception e) {
	        log.error("Failed to send push notification: id={}, error={}", 
	                notification.getId(), e.getMessage());
	        throw e;
	    }
	}

	@Override
	@Async("notificationExecutor")
	@Transactional
	public void sendInAppNotification(Notification notification) {
		log.info("Sending in-app notification: id={}", notification.getId());

		// In-app notifications are just stored in DB and polled by frontend
		updateNotificationStatus(notification.getId(), NotificationStatus.SENT);
	}

	// Fallback methods
	private void emailFallback(Notification notification, Exception e) {
		log.error("Email fallback triggered for notification: {}", notification.getId());
		updateNotificationStatus(notification.getId(), NotificationStatus.FAILED);
	}

	private void smsFallback(Notification notification, Exception e) {
		log.error("SMS fallback triggered for notification: {}", notification.getId());
		updateNotificationStatus(notification.getId(), NotificationStatus.FAILED);
	}

	private void pushFallback(Notification notification, Exception e) {
		log.error("Push fallback triggered for notification: {}", notification.getId());
		updateNotificationStatus(notification.getId(), NotificationStatus.FAILED);
	}

	@Transactional
	private void updateNotificationStatus(Long notificationId, NotificationStatus status) {
		Notification notification = notificationRepository.findById(notificationId).orElseThrow();
		notification.setStatus(status);
		if (status == NotificationStatus.SENT) {
			notification.setSentAt(LocalDateTime.now());
		}
		notificationRepository.save(notification);
	}
}
