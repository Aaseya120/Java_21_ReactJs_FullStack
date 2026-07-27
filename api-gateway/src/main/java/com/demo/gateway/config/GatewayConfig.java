package com.demo.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

/**
 * Gateway configuration beans.
 *
 * <p>
 * Redis reactive connection is auto-configured by Spring Boot from
 * {@code spring.data.redis.*} properties — no manual bean needed.
 */
@Configuration
public class GatewayConfig {

	/**
	 * Rate limit key resolver — buckets by client IP address. Referenced in
	 * application.yml as {@code #{@ipKeyResolver}}.
	 */
	@Bean
	public KeyResolver ipKeyResolver() {
		return exchange -> {
			String ip = exchange.getRequest().getRemoteAddress() != null
					? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
					: "unknown";
			return Mono.just(ip);
		};
	}
}
