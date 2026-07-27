package com.demo.common.audit;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to asynchronously persist LOG_REST audit records with sensitive data masking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogRestService {

	private final LogRestRepository logRestRepository;

	@Value("${spring.application.name:unknown-service}")
	private String serviceName;

	private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
			"\"(password|token|refreshToken|secret|credentials)\"\\s*:\\s*\"[^\"]*\"",
			Pattern.CASE_INSENSITIVE);

	@Async
	public void logRestCall(String userId, OffsetDateTime timestamp, String httpMethod, String requestUrl,
			String rawRequestBody, String rawResponseBody, Integer statusCode, String errorCode, String errorDesc,
			String requestId, String clientIp, String userAgent, Long durationMs) {
		try {
			String sanitizedReq = sanitizeJson(rawRequestBody);
			String sanitizedResp = sanitizeJson(rawResponseBody);

			LogRest logRest = LogRest.builder()
					.userId(userId != null && !userId.isBlank() ? userId : "ANONYMOUS")
					.timestamp(timestamp != null ? timestamp : OffsetDateTime.now())
					.httpMethod(httpMethod != null ? httpMethod : "?")
					.requestUrl(requestUrl != null ? requestUrl : "")
					.requestBody(sanitizedReq)
					.responseBody(sanitizedResp)
					.statusCode(statusCode != null ? statusCode : 500)
					.errorCode(errorCode)
					.errorDesc(errorDesc)
					.requestId(requestId != null && !requestId.isBlank() ? requestId : UUID.randomUUID().toString())
					.serviceName(serviceName)
					.clientIp(clientIp != null ? clientIp : "?")
					.userAgent(userAgent != null ? userAgent : "?")
					.durationMs(durationMs != null ? durationMs : 0L)
					.build();

			logRestRepository.save(logRest);
		} catch (Exception ex) {
			log.error("Failed to save LOG_REST entry: {}", ex.getMessage());
		}
	}

	private String sanitizeJson(String payload) {
		if (payload == null || payload.isBlank()) {
			return payload;
		}
		// Mask passwords and tokens
		String masked = SENSITIVE_PATTERN.matcher(payload).replaceAll("\"$1\":\"***\"");
		// Truncate to 10,000 characters if too large
		if (masked.length() > 10000) {
			return masked.substring(0, 10000) + "... [TRUNCATED]";
		}
		return masked;
	}
}
