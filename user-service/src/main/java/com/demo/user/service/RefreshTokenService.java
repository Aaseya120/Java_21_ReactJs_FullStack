package com.demo.user.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.user.entity.RefreshToken;
import com.demo.user.entity.User;
import com.demo.user.exception.TokenRefreshException;
import com.demo.user.repository.RefreshTokenRepository;
import com.demo.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	@Value("${jwt.refreshExpiration:604800000}") // Default 7 days
	private Long refreshTokenDurationMs;

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final EntityManager entityManager;

	public java.util.Optional<RefreshToken> findByToken(String token) {
		return refreshTokenRepository.findByToken(token);
	}

	@Transactional
	public RefreshToken createRefreshToken(Long userId) {
		User user = userRepository.findById(userId).orElseThrow();

		// Remove existing refresh token for this user to invalidate old sessions.
		// Flush immediately so PostgreSQL processes the DELETE before the INSERT
		// below — otherwise the unique constraint on user_id fires prematurely.
		refreshTokenRepository.deleteByUser(user);
		entityManager.flush();

		RefreshToken refreshToken = RefreshToken.builder().user(user).token(UUID.randomUUID().toString())
				.expiryDate(Instant.now().plusMillis(refreshTokenDurationMs)).build();

		return refreshTokenRepository.save(refreshToken);
	}

	public RefreshToken verifyExpiration(RefreshToken token) {
		if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
			refreshTokenRepository.delete(token);
			throw new TokenRefreshException("Refresh token was expired. Please make a new signin request");
		}
		return token;
	}

	@Transactional
	public int deleteByUserId(Long userId) {
		return userRepository.findById(userId).map(refreshTokenRepository::deleteByUser).orElse(0);
	}

	@Transactional
	public void deleteByToken(String token) {
		refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
	}
}

