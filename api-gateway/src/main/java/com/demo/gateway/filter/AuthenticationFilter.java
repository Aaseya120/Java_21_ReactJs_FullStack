package com.demo.gateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import com.demo.gateway.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Global authentication filter applied to EVERY inbound request.
 *
 * <p>
 * Open endpoints (e.g. /auth/**) are whitelisted and pass through. All other
 * requests must carry a valid Bearer JWT in the Authorization header. On
 * success the filter forwards X-Auth-User and X-Auth-Role headers to downstream
 * services so they can trust the caller's identity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

	private final JwtUtil jwtUtil;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/** Paths that bypass JWT authentication */
	private static final List<String> OPEN_ENDPOINTS = List.of(
			"/api/v1/auth/register",
			"/api/v1/auth/login",
			"/api/v1/auth/refresh",
			"/api/v1/auth/recover-id",
			"/actuator/**");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getURI().getPath();

		// ── Check if endpoint is public ─────────────────────
		if (isOpenEndpoint(path)) {
			return chain.filter(exchange);
		}

		// ── Validate Authorization header or query parameter ───────────────────
		String token = null;
		String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		} else {
			token = request.getQueryParams().getFirst("token");
		}

		if (token == null || token.isEmpty()) {
			log.warn("Missing Authorization header and token parameter for path: {}", path);
			return onAuthFailure(exchange, "Missing token");
		}

		if (!jwtUtil.isTokenValid(token)) {
			log.warn("Invalid JWT token for path: {}", path);
			return onAuthFailure(exchange, "Invalid or expired token");
		}

		// ── Enrich request with user identity headers ────────
		String username = jwtUtil.extractUsername(token);
		String role = jwtUtil.extractAllClaims(token).get("role", String.class);
		String userId = jwtUtil.extractAllClaims(token).get("userId", String.class);

		ServerHttpRequest mutated = request.mutate()
				.header("X-Auth-User", username)
				.header("X-Auth-Role", role != null ? role : "")
				.header("X-Auth-UserId", userId != null ? userId : "")
				.build();

		log.debug("Authenticated request — user={}, path={}", username, path);
		return chain.filter(exchange.mutate().request(mutated).build());
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	// ── Helpers ─────────────────────────────────────────────

	private boolean isOpenEndpoint(String path) {
		return OPEN_ENDPOINTS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
	}

	private Mono<Void> onAuthFailure(ServerWebExchange exchange, String reason) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().add("X-Auth-Error", reason);
		return response.setComplete();
	}
}
