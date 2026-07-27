package com.demo.notification.dto;

/**
 * Notification delivery channel — sealed interface pattern. Demonstrates Java
 * 21 sealed types for exhaustive type safety.
 */
public enum NotificationType {
	EMAIL, SMS, PUSH, IN_APP
}
