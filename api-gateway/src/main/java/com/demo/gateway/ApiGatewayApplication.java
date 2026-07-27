package com.demo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway — the single entry point for all client requests.
 *
 * <p>
 * Routes traffic to downstream services using direct URLs resolved by
 * Kubernetes DNS. No Eureka or service registry needed — K8s Services handle
 * discovery and load balancing natively.
 */
@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
