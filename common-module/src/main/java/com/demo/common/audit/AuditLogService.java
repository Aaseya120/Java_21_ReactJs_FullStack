package com.demo.common.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	@Value("${spring.application.name:unknown-service}")
	private String serviceName;

	@Async
	public void log(String action, String entityType, String entityId, String userId, String userIp, String httpMethod,
			String requestUri, Integer responseStatus, Long durationMs, String details) {
		try {
			auditLogRepository.save(AuditLog.builder().serviceName(serviceName).action(action).entityType(entityType)
					.entityId(entityId).userId(userId).userIp(userIp).httpMethod(httpMethod).requestUri(requestUri)
					.responseStatus(responseStatus).durationMs(durationMs).details(details).build());
		} catch (Exception ex) {
			log.error("Failed to save audit log: {}", ex.getMessage());
		}
	}
}
