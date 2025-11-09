package com.transaction_service.enums;

public enum NotificationStatus {
	PENDING, // Notification queued
	SENT, // Successfully sent
	DELIVERED, // Delivered to recipient
	FAILED, // Failed to send
	BOUNCED, // Bounced back
	RETRY // Scheduled for retry
}
