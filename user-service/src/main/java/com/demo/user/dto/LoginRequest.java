package com.demo.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login requests — Java 21 record.
 */
public record LoginRequest(

		@NotBlank(message = "Email or User ID is required") String email,

		@NotBlank(message = "Password is required") String password) {
}
