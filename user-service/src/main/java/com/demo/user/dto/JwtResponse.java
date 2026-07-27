package com.demo.user.dto;

/**
 * JWT token response — returned on successful login or token refresh. Java 21
 * record.
 */
public record JwtResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
		UserResponse user) {
	/** Convenience factory with default token type. */
	public static JwtResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
		return new JwtResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
	}
}
