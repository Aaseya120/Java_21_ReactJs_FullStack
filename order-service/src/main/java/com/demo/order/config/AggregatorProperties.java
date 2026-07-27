package com.demo.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aggregator")
public record AggregatorProperties(String orderServiceUrl, String productServiceUrl) {
	public AggregatorProperties {
		if (orderServiceUrl == null || orderServiceUrl.isBlank()) {
			orderServiceUrl = "http://localhost:8082/api/v1/orders/";
		}
		if (productServiceUrl == null || productServiceUrl.isBlank()) {
			productServiceUrl = "http://localhost:8083/api/v1/products/";
		}
	}
}
