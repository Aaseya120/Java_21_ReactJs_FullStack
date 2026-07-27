package com.demo.order.controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.http.ResponseEntity;
import com.demo.order.exception.OrderAccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.demo.common.constant.ApiConstants;
import com.demo.common.dto.ApiResponse;
/**
 * Demonstrates the Aggregator Pattern.
 * Gathers data from multiple microservices concurrently and combines them.
 */
import com.demo.order.config.AggregatorProperties;
import com.demo.order.exception.OrderNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.AggregatorApi.BASE)
@RequiredArgsConstructor
public class AggregatorController {

	private final RestClient restClient;
	private final AggregatorProperties aggregatorProperties;

	@SuppressWarnings("unchecked")
	@GetMapping(ApiConstants.AggregatorApi.ORDER_DETAILS)
	public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> getOrderDetails(
			@PathVariable Long orderId,
			@RequestHeader(value = "X-Auth-UserId", required = false) String authUserId,
			@RequestHeader(value = "X-Auth-Role", required = false) String authRole) {

		return CompletableFuture.supplyAsync(
				() -> restClient.get()
						.uri(aggregatorProperties.orderServiceUrl() + orderId)
						.header("X-Auth-UserId", authUserId != null ? authUserId : "")
						.header("X-Auth-Role", authRole != null ? authRole : "")
						.retrieve()
						.body(Map.class))
				.thenCompose(orderResp -> {
					Map<String, Object> data = (Map<String, Object>) orderResp.get("data");
					if (data == null) {
						throw new OrderNotFoundException(String.valueOf(orderId));
					}
					if (!"ADMIN".equalsIgnoreCase(authRole) && authUserId != null && !authUserId.isBlank()) {
						String orderUserIdStr = String.valueOf(data.get("userId"));
						if (!orderUserIdStr.equals(authUserId)) {
							throw new OrderAccessDeniedException("Not authorized to access orders of other users");
						}
					}
					String productIdStr = String.valueOf(data.get("productId"));

					CompletableFuture<Map<String, Object>> productFuture = CompletableFuture.supplyAsync(() -> restClient.get()
							.uri(aggregatorProperties.productServiceUrl() + productIdStr)
							.header("X-Auth-UserId", authUserId != null ? authUserId : "")
							.header("X-Auth-Role", authRole != null ? authRole : "")
							.retrieve()
							.body(Map.class));

					return productFuture.thenApply(productResp -> {
						Map<String, Object> aggregated = Map.of("order", orderResp, "product", productResp);
						return ResponseEntity.ok(ApiResponse.success(aggregated));
					});
				}).exceptionally(ex -> {
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					if (cause instanceof OrderAccessDeniedException accessEx) {
						return ResponseEntity.status(403)
								.body(ApiResponse.error("FORBIDDEN", accessEx.getMessage()));
					}
					if (cause instanceof RestClientResponseException restEx) {
						int status = restEx.getStatusCode().value();
						String errorCode = status == 404 ? "NOT_FOUND" : (status == 403 ? "FORBIDDEN" : "AGGREGATOR_ERROR");
						String errorDesc = status == 404 ? "Order not found: " + orderId : restEx.getStatusText();
						try {
							JsonNode node = new ObjectMapper().readTree(restEx.getResponseBodyAsString());
							if (node.has("errorCode") && !node.get("errorCode").isNull()) {
								errorCode = node.get("errorCode").asText();
							}
							if (node.has("errorDesc") && !node.get("errorDesc").isNull()) {
								errorDesc = node.get("errorDesc").asText();
							}
						} catch (Exception ignored) {
						}
						return ResponseEntity.status(status).body(ApiResponse.error(errorCode, errorDesc));
					}
					if (cause instanceof OrderNotFoundException notFoundEx) {
						return ResponseEntity.status(404)
								.body(ApiResponse.error("ORDER_NOT_FOUND", notFoundEx.getMessage()));
					}
					return ResponseEntity.status(500).body(ApiResponse.error("INTERNAL_ERROR", cause.getMessage()));
				});
	}
}
