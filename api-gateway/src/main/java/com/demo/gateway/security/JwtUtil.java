package com.demo.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT utility shared between the Gateway and individual services. Validates
 * tokens; does NOT issue them (that's the User Service's job).
 */
@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	/**
	 * Derives the signing key from the configured secret.
	 */
	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * Parses all claims from a JWT (throws if invalid/expired).
	 */
	public Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
	}

	/**
	 * Extracts a specific claim from the token.
	 */
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		return claimsResolver.apply(extractAllClaims(token));
	}

	/**
	 * Extracts the subject (username/email) from the token.
	 */
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	/**
	 * Returns true if the token can be parsed without error.
	 */
	public boolean isTokenValid(String token) {
		try {
			extractAllClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
