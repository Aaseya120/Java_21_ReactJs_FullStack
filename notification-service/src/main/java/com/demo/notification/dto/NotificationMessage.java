package com.demo.notification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification payload — Java 21 record.
 *
 * <p>
 * Represents a generic notification to be sent to a user.
 */
public record NotificationMessage(String notificationId, String recipientId, String recipientEmail, String subject,
		String body, NotificationType type, Instant createdAt) {
	public static NotificationMessage of(String recipientId, String recipientEmail, String subject, String body,
			NotificationType type) {
		return new NotificationMessage(UUID.randomUUID().toString(), recipientId, recipientEmail, subject,
				body, type, Instant.now());
	}
}
