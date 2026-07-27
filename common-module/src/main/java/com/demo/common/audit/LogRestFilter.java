package com.demo.common.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter to capture full REST API transactions into LOG_REST table.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@Slf4j
public class LogRestFilter extends OncePerRequestFilter {

	private final LogRestService logRestService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri.startsWith("/actuator") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long startMs = System.currentTimeMillis();
		OffsetDateTime timestamp = OffsetDateTime.now();

		ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request);
		ContentCachingResponseWrapper respWrapper = new ContentCachingResponseWrapper(response);

		String requestId = request.getHeader("X-Request-Id");
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		}

		try {
			filterChain.doFilter(reqWrapper, respWrapper);
		} finally {
			long durationMs = System.currentTimeMillis() - startMs;
			try {
				recordAudit(reqWrapper, respWrapper, timestamp, requestId, durationMs);
			} catch (Exception ex) {
				log.error("Error recording LOG_REST audit: {}", ex.getMessage());
			}
			respWrapper.copyBodyToResponse();
		}
	}

	private void recordAudit(ContentCachingRequestWrapper req, ContentCachingResponseWrapper resp,
			OffsetDateTime timestamp, String requestId, long durationMs) {
		String method = req.getMethod();
		String url = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
		String clientIp = req.getHeader("X-Forwarded-For");
		if (clientIp != null && clientIp.contains(",")) {
			clientIp = clientIp.split(",")[0].trim();
		} else if (clientIp == null) {
			clientIp = req.getRemoteAddr();
		}
		String userAgent = req.getHeader("User-Agent");

		String reqBody = new String(req.getContentAsByteArray(), StandardCharsets.UTF_8);
		String respBody = new String(resp.getContentAsByteArray(), StandardCharsets.UTF_8);

		// Extract userId
		String userId = req.getHeader("X-Auth-User");
		if ((userId == null || userId.isBlank()) && reqBody.contains("\"email\"")) {
			// Extract email from login/register body if present
			userId = extractJsonField(reqBody, "email");
		}
		if (userId == null || userId.isBlank()) {
			userId = "ANONYMOUS";
		}

		int status = resp.getStatus();
		String errorCode = extractJsonField(respBody, "errorCode");
		String errorDesc = extractJsonField(respBody, "errorDesc");

		if (status >= 400 && (errorCode == null || errorCode.isBlank())) {
			errorCode = "HTTP_" + status;
		}

		logRestService.logRestCall(userId, timestamp, method, url, reqBody, respBody, status, errorCode, errorDesc,
				requestId, clientIp, userAgent, durationMs);
	}

	private String extractJsonField(String json, String field) {
		if (json == null || json.isBlank()) return null;
		try {
			String token = "\"" + field + "\"";
			int idx = json.indexOf(token);
			if (idx != -1) {
				int colonIdx = json.indexOf(':', idx + token.length());
				if (colonIdx != -1) {
					int startQuote = json.indexOf('"', colonIdx);
					if (startQuote != -1) {
						int endQuote = json.indexOf('"', startQuote + 1);
						if (endQuote != -1) {
							return json.substring(startQuote + 1, endQuote);
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}
}
