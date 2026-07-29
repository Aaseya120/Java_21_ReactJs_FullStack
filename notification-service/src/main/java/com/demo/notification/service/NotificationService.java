package com.demo.notification.service;

import org.springframework.stereotype.Service;

import com.demo.notification.dto.NotificationMessage;
import com.demo.notification.dto.NotificationType;
import com.demo.notification.controller.ReactiveNotificationController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Notification dispatch service.
 *
 * <p>
 * Uses Java 21 sealed interfaces / pattern matching switch to route to the
 * correct delivery channel. Currently logs all notifications; add channel
 * implementations (email, SMS, push) as needed.
 *
 * <p>
 * Follows the Open/Closed Principle — new channels added by extending
 * {@link NotificationType} and adding a case here, without modifying existing
 * logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	/**
	 * Dispatches a notification to the appropriate channel. Demonstrates Java 21
	 * pattern matching switch.
	 */
	public void dispatch(NotificationMessage message) {
		log.info("Dispatching notification: type={}, recipient={}, subject={}", message.type(), message.recipientId(),
				message.subject());

		// Java 21 pattern matching switch — exhaustive over NotificationType
		switch (message.type()) {
		case EMAIL -> sendEmail(message);
		case SMS -> sendSms(message);
		case PUSH -> sendPushNotification(message);
		case IN_APP -> storeInAppNotification(message);
		}
	}

	// ── Channel implementations ──────────────────────────────

	private final ReactiveNotificationController reactiveNotificationController;
	private final org.springframework.mail.javamail.JavaMailSender mailSender;

	private void sendEmail(NotificationMessage message) {
		log.info("""
				[EMAIL] To: {}
				Subject: {}
				Body: {}
				""", message.recipientEmail(), message.subject(), message.body());
		
		try {
			org.springframework.mail.SimpleMailMessage mailMessage = new org.springframework.mail.SimpleMailMessage();
			mailMessage.setTo(message.recipientEmail());
			mailMessage.setSubject(message.subject());
			mailMessage.setText(message.body());
			mailMessage.setFrom("bandi00545@gmail.com"); // Usually should match MAIL_USER
			mailSender.send(mailMessage);
			log.info("Email sent successfully to {}", message.recipientEmail());
		} catch (Exception e) {
			log.error("Failed to send email to {}", message.recipientEmail(), e);
		}
	}

	private void sendSms(NotificationMessage message) {
		// In production: integrate Twilio or AWS SNS
		log.info("[SMS] To: {}, Message: {}", message.recipientId(), message.body());

	}

	private void sendPushNotification(NotificationMessage message) {
		// In production: integrate Firebase Cloud Messaging or APNs
		log.info("[PUSH] To: {}, Title: {}", message.recipientId(), message.subject());

	}

	private void storeInAppNotification(NotificationMessage message) {
		// In production: persist to notifications table and expose via WebSocket
		log.info("[IN_APP] Stored for user: {}, body: {}", message.recipientId(), message.body());
		reactiveNotificationController.sendToUser(message.recipientId(), message.body());
	}

	// ── Template builders ────────────────────────────────────

	/**
	 * Builds the email body for an order event. Showcases Java 21 text block
	 * templates.
	 */
	public String buildOrderEmailBody(String eventType, String orderId, String status) {
		return switch (eventType) {
		case "ORDER_CREATED" -> """
				Dear customer,

				Your order #%s has been successfully placed!
				Current status: %s

				We will notify you as the order progresses.

				Thank you for shopping with us!
				""".formatted(orderId, status);

		case "ORDER_STATUS_CHANGED" -> """
				Dear customer,

				Your order #%s status has been updated to: %s

				Track your order in the app for real-time updates.
				""".formatted(orderId, status);

		case "ORDER_CANCELLED" -> """
				Dear customer,

				Your order #%s has been cancelled.
				If you did not request this cancellation, please contact support.
				""".formatted(orderId);
				
		case "ORDER_REFUNDED" -> """
				Dear customer,

				Good news! A refund has been issued for your order #%s.
				The amount has been credited back to your original payment method or wallet according to our policies.

				If you have any questions, please contact our support team.
				""".formatted(orderId);

		default -> "Order #%s update: %s".formatted(orderId, eventType);
		};
	}
}
