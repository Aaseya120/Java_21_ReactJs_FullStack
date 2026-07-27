package com.demo.gateway.exception;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for the reactive gateway. Returns structured JSON
 * error responses instead of the default whitelabel page.
 */
@Component
@Order(-2)
@Slf4j
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		log.error("Gateway error — {}: {}", ex.getClass().getSimpleName(), ex.getMessage());

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		String message = "An unexpected error occurred";

		if (ex instanceof ResponseStatusException rse) {
			status = HttpStatus.valueOf(rse.getStatusCode().value());
			message = rse.getReason() != null ? rse.getReason() : ex.getMessage();
		}

		exchange.getResponse().setStatusCode(status);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

		String body;
		try {
			body = objectMapper
					.writeValueAsString(Map.of("success", false, "message", message, "status", status.value()));
		} catch (Exception jsonEx) {
			body = "{\"success\":false,\"message\":\"An unexpected error occurred\",\"status\":500}";
		}

		DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

		return exchange.getResponse().writeWith(Mono.just(buffer));
	}
}
