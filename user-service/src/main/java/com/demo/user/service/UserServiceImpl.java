package com.demo.user.service;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.user.dto.UserResponse;
import com.demo.user.entity.User;
import com.demo.user.exception.UserNotFoundException;
/**
 * Core business logic for user management and authentication.
 *
 * <p>Implements {@link UserDetailsService} so Spring Security can load users
 * directly from this service during JWT filter processing.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP — only manages users; JWT issuance is delegated to {@link JwtUtil}</li>
 *   <li>OCP — new user types can be added via {@link Role} without modifying this class</li>
 *   <li>DIP — depends on repository/util abstractions, not concrete implementations</li>
 * </ul>
 */
import com.demo.user.mapper.UserMapper;
import com.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService, UserDetailsService {

	private static final String USER_CACHE = "users";

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	// ── Profile Management ──────────────────────────────────

	/**
	 * Retrieves a user profile, checking Redis cache first.
	 */
	@Cacheable(value = USER_CACHE, key = "#id")
	public UserResponse getUserById(Long id) {
		return userRepository.findById(id).map(userMapper::toResponse)
				.orElseThrow(() -> new UserNotFoundException(id.toString()));
	}

	/**
	 * Updates the user's full name. Evicts the Redis cache entry.
	 */
	@Transactional
	@CacheEvict(value = USER_CACHE, key = "#id")
	public UserResponse updateUser(Long id, String newFullName) {
		User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id.toString()));
		user.setFullName(newFullName);
		return userMapper.toResponse(userRepository.save(user));
	}

	// ── UserDetailsService ──────────────────────────────────

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username != null && username.matches("\\d+")) {
			try {
				return userRepository.findById(Long.valueOf(username))
						.orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + username));
			} catch (NumberFormatException ignored) {
				// fall through to email lookup
			}
		}
		return userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}

}

