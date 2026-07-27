package com.demo.common.dto;

/**
 * Generic API response wrapper — Java 21 record with a generic type parameter.
 * All controllers return this to ensure a consistent response envelope.
 *
 * @param <T> the data payload type
 */
public record ApiResponse<T>(String errorCode, String errorDesc, boolean isClean, T data) {
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(null, null, true, data);
	}

	// Keep this for backwards compatibility where message was passed, mapping
	// message to nowhere or keeping it as ignored
	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(null, null, true, data);
	}

	public static <T> ApiResponse<T> error(String errorCode, String errorDesc) {
		return new ApiResponse<>(errorCode, errorDesc, false, null);
	}

	// Overload for when only message is provided
	public static <T> ApiResponse<T> error(String errorDesc) {
		return new ApiResponse<>("ERROR", errorDesc, false, null);
	}
}
