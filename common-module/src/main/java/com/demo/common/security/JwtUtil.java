package com.demo.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT utility — issues and validates JSON Web Tokens using HMAC-SHA256.
 *
 * <p>
 * Follows the Single Responsibility Principle: this class ONLY handles JWT
 * operations. It does not touch HTTP, Spring Security, or business logic.
 */
@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.expiration:86400000}")
	private long jwtExpiration;

	// ── Token Generation ────────────────────────────────────

	/**
	 * Generates an access token for the given user payload.
	 */
	public String generateToken(String email, String role, String userId) {
		Map<String, Object> extraClaims = new HashMap<>();
		extraClaims.put("role", role);
		extraClaims.put("userId", userId);
		return buildToken(extraClaims, email, jwtExpiration);
	}

	private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
		return Jwts.builder().claims(extraClaims).subject(subject).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + expiration)).signWith(getSigningKey()).compact();
	}

	// ── Token Validation ────────────────────────────────────

	public boolean isTokenValid(String token, String userEmail) {
		final String username = extractUsername(token);
		return username.equals(userEmail) && !isTokenExpired(token);
	}

	public boolean isTokenValid(String token) {
		try {
			return !isTokenExpired(token);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	// ── Claims Extraction ───────────────────────────────────

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> resolver) {
		return resolver.apply(extractAllClaims(token));
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}
}
