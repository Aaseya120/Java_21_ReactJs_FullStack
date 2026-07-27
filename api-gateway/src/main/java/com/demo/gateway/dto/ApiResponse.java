package com.demo.gateway.dto;

public record ApiResponse<T>(String errorCode, String errorDesc, boolean isClean, T data) {
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(null, null, true, data);
	}

	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(null, null, true, data);
	}

	public static <T> ApiResponse<T> error(String errorCode, String errorDesc) {
		return new ApiResponse<>(errorCode, errorDesc, false, null);
	}

	public static <T> ApiResponse<T> error(String errorDesc) {
		return new ApiResponse<>("ERROR", errorDesc, false, null);
	}
}
