package com.demo.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.gateway.dto.ApiResponse;

import reactor.core.publisher.Mono;

/**
 * Fallback controller invoked by circuit breakers when a downstream service is
 * unavailable.
 *
 * <p>Uses {@code @RequestMapping} without a method restriction so that POST,
 * PUT, and DELETE circuit-breaker fallbacks (e.g., /register, /login) are
 * handled correctly. A {@code @GetMapping} would return 405 for non-GET fallbacks.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

	@RequestMapping("/user")
	public Mono<ResponseEntity<ApiResponse<Object>>> userFallback() {
		return fallbackResponse("User Service is temporarily unavailable. Please try again later.");
	}

	@RequestMapping("/order")
	public Mono<ResponseEntity<ApiResponse<Object>>> orderFallback() {
		return fallbackResponse("Order Service is temporarily unavailable. Please try again later.");
	}

	@RequestMapping("/product")
	public Mono<ResponseEntity<ApiResponse<Object>>> productFallback() {
		return fallbackResponse("Product Service is temporarily unavailable. Please try again later.");
	}

	@RequestMapping("/notification")
	public Mono<ResponseEntity<ApiResponse<Object>>> notificationFallback() {
		return fallbackResponse("Notification Service is temporarily unavailable. Please try again later.");
	}

	private Mono<ResponseEntity<ApiResponse<Object>>> fallbackResponse(String message) {
		return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.error("SERVICE_UNAVAILABLE", message)));
	}
}
