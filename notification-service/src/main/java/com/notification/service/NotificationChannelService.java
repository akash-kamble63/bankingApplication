package com.notification.service;

import com.notification.entity.Notification;

public interface NotificationChannelService {
	void sendEmail(Notification notification);
    void sendSms(Notification notification);
    void sendPushNotification(Notification notification);
    void sendInAppNotification(Notification notification);
}
	