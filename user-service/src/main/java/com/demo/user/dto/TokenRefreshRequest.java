package com.demo.user.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank(message = "Refresh token cannot be blank") String refreshToken) {
}
