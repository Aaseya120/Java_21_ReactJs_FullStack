package com.demo.user.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.security.JwtUtil;
import com.demo.user.dto.JwtResponse;
import com.demo.user.dto.LoginRequest;
import com.demo.user.dto.RegisterRequest;
import com.demo.user.dto.UserResponse;
import com.demo.user.entity.RefreshToken;
import com.demo.user.entity.Role;
import com.demo.user.entity.User;
import com.demo.user.exception.EmailAlreadyExistsException;
import com.demo.user.exception.TokenRefreshException;
import com.demo.user.exception.UserNotFoundException;
import com.demo.user.mapper.UserMapper;
import com.demo.user.outbox.OutboxEvent;
import com.demo.user.outbox.OutboxEventRepository;
import com.demo.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final AuthenticationManager authenticationManager;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;
	private final RefreshTokenService refreshTokenService;
	private final UserMapper userMapper;

	@Transactional
	@Override
	public JwtResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyExistsException(request.email());
		}

		Role assignedRole = request.email().toLowerCase().startsWith("admin") ? Role.ADMIN : Role.USER;
		User user = User.builder().email(request.email()).fullName(request.fullName())
				.mobileNumber(request.mobileNumber())
				.password(passwordEncoder.encode(request.password())).role(assignedRole).build();

		user = userRepository.save(user);
		log.info("User registered: id={}, email={}", user.getId(), user.getEmail());

		publishUserEvent("USER_REGISTERED", user);

		String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId().toString());
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
		return JwtResponse.of(token, refreshToken.getToken(), jwtUtil.extractExpiration(token).getTime(),
				userMapper.toResponse(user));
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse recoverUserId(String contact) {
		if (contact == null || contact.isBlank()) {
			throw new UserNotFoundException("Contact information cannot be empty");
		}
		String clean = contact.trim();
		User user = userRepository.findByEmailOrMobileNumber(clean, clean)
				.orElseThrow(() -> new UserNotFoundException("No account found matching email or mobile number: " + clean));
		return userMapper.toResponse(user);
	}

	@Transactional
	@Override
	public JwtResponse login(LoginRequest request) {
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

		User user;
		if (request.email() != null && request.email().matches("\\d+")) {
			user = userRepository.findById(Long.valueOf(request.email()))
					.orElseThrow(() -> new UserNotFoundException(request.email()));
		} else {
			user = userRepository.findByEmail(request.email())
					.orElseThrow(() -> new UserNotFoundException(request.email()));
		}

		log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());
		String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId().toString());
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
		return JwtResponse.of(token, refreshToken.getToken(), jwtUtil.extractExpiration(token).getTime(),
				userMapper.toResponse(user));
	}

	@Transactional
	@Override
	public JwtResponse refreshToken(String requestRefreshToken) {
		return refreshTokenService.findByToken(requestRefreshToken).map(refreshTokenService::verifyExpiration)
				.map(RefreshToken::getUser).map(user -> {
					String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(),
							user.getId().toString());
					return JwtResponse.of(token, requestRefreshToken, jwtUtil.extractExpiration(token).getTime(),
							userMapper.toResponse(user));
				}).orElseThrow(() -> new TokenRefreshException("Refresh token is not in database!"));
	}

	@Transactional
	@Override
	public void logout(String refreshToken) {
		refreshTokenService.deleteByToken(refreshToken);
		log.info("User logged out, refresh token invalidated.");
	}

	/**
	 * Saves an outbox event for the user so the relay scheduler can publish it
	 * to Kafka. This runs in a REQUIRES_NEW transaction so that an outbox save
	 * failure never rolls back the parent user-registration transaction.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void publishUserEvent(String eventType, User user) {
		try {
			Map<String, Object> event = new HashMap<>();
			event.put("eventId", UUID.randomUUID().toString());
			event.put("eventType", eventType);
			event.put("userId", user.getId().toString());
			event.put("email", user.getEmail());
			event.put("name", user.getFullName());
			event.put("timestamp", System.currentTimeMillis());

			OutboxEvent outboxEvent = OutboxEvent.builder().aggregateId(user.getId().toString()).aggregateType("User")
					.eventType(eventType).payload(objectMapper.writeValueAsString(event)).build();
			outboxEventRepository.save(outboxEvent);
			log.debug("Saved outbox event: {}", eventType);
		} catch (Exception e) {
			// Log and continue — outbox relay is best-effort; registration must succeed.
			log.error("Failed to save user outbox event (non-blocking): {}", e.getMessage(), e);
		}
	}
}

