package com.demo.common.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

	private final AuditLogService auditLogService;

	// Apply to any class annotated with @RestController within the com.demo package
	// structure
	@Around("@within(org.springframework.web.bind.annotation.RestController)")
	public Object audit(ProceedingJoinPoint jp) throws Throwable {
		long start = System.currentTimeMillis();
		HttpServletRequest req = getRequest();
		String method = req != null ? req.getMethod() : "?";
		String uri = req != null ? req.getRequestURI() : "?";
		String ip = req != null ? getIp(req) : "?";
		String user = req != null ? req.getHeader("X-Auth-User") : null;

		Object result;
		int status = 200;
		try {
			result = jp.proceed();
			if (result instanceof ResponseEntity<?> re) {
				status = re.getStatusCode().value();
			}
		} catch (Throwable ex) {
			auditLogService.log(jp.getSignature().getName(), "System", null, user, ip, method, uri, 500,
					System.currentTimeMillis() - start, ex.getMessage());
			throw ex;
		}

		auditLogService.log(jp.getSignature().getName(), "System", null, user, ip, method, uri, status,
				System.currentTimeMillis() - start, null);
		return result;
	}

	private HttpServletRequest getRequest() {
		var a = RequestContextHolder.getRequestAttributes();
		return a instanceof ServletRequestAttributes s ? s.getRequest() : null;
	}

	private String getIp(HttpServletRequest r) {
		String f = r.getHeader("X-Forwarded-For");
		return f != null ? f.split(",")[0].trim() : r.getRemoteAddr();
	}
}
