package com.demo.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration — uses Java 21 record syntax. Bean Validation
 * annotations are applied directly on record components.
 */
public record RegisterRequest(

		@NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") String fullName,

		@NotBlank(message = "Email is required") @Email(message = "Email must be a valid address") String email,

		@NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,

		String mobileNumber) {
}
