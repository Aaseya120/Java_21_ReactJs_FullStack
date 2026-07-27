package com.demo.user.dto;

import java.time.OffsetDateTime;

import com.demo.user.entity.Role;

/**
 * Public-facing user profile DTO — never exposes password hash. Java 21 record
 * for immutable data transfer.
 */
public record UserResponse(Long id, String email, String fullName, String mobileNumber, Role role, boolean enabled,
		OffsetDateTime createdAt) {
}

