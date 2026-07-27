package com.demo.notification.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.dto.ApiResponse;

/**
 * Simple health/status controller for the Notification Service.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	@GetMapping("/status")
	public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
		return ResponseEntity.ok(ApiResponse.success(Map.of("service", "notification-service", "status", "RUNNING",
				"message", "Kafka consumers are active")));
	}
}
