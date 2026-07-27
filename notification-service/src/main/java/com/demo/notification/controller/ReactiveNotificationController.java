package com.demo.notification.controller;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.constant.ApiConstants;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Spring WebFlux Reactive Controller for real-time notifications via
 * Server-Sent Events (SSE).
 */
@RestController
@RequestMapping(ApiConstants.NotificationApi.BASE)
public class ReactiveNotificationController {

	// Sinks for each user to broadcast events asynchronously
	private final ConcurrentHashMap<String, Sinks.Many<String>> userSinks = new ConcurrentHashMap<>();

	/**
	 * Subscribes a client to a real-time SSE stream.
	 * 
	 * @param userId The ID of the user connecting to the stream.
	 * @return A Flux of ServerSentEvents.
	 */
	@GetMapping(path = ApiConstants.NotificationApi.STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamNotifications(@PathVariable String userId) {
		Sinks.Many<String> sink = userSinks.computeIfAbsent(userId,
				id -> Sinks.many().multicast().onBackpressureBuffer());

		// Keep connection alive with heartbeat every 30 seconds
		Flux<ServerSentEvent<String>> keepAlive = Flux.interval(Duration.ofSeconds(30))
				.map(seq -> ServerSentEvent.<String>builder().event("ping").data("heartbeat").build());

		Flux<ServerSentEvent<String>> notifications = sink.asFlux().map(message -> ServerSentEvent.<String>builder()
				.id(UUID.randomUUID().toString()).event("notification").data(message).build());

		return Flux.merge(keepAlive, notifications).doOnCancel(() -> userSinks.remove(userId));
	}

	/**
	 * Helper to push a notification to a specific user's sink. This would typically
	 * be called by the Kafka Listener when an event arrives.
	 */
	public void sendToUser(String userId, String message) {
		Sinks.Many<String> sink = userSinks.get(userId);
		if (sink != null) {
			sink.tryEmitNext(message);
		}
	}
}

