package com.demo.user.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.demo.common.security.JwtUtil;
import com.demo.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT authentication filter — runs once per request.
 *
 * <p>
 * Extracts the Bearer token from the Authorization header, validates it, and
 * loads the user into the Spring Security context so downstream code can call
 * {@code SecurityContextHolder.getContext().getAuthentication()}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");

		// Skip if no Bearer token present
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		final String jwt = authHeader.substring(7);
		final String userEmail;

		try {
			userEmail = jwtUtil.extractUsername(jwt);
		} catch (Exception e) {
			log.warn("Failed to extract username from JWT: {}", e.getMessage());
			filterChain.doFilter(request, response);
			return;
		}

		// Only authenticate if not already done
		if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = userRepository.findByEmail(userEmail).orElse(null);

			if (userDetails != null && jwtUtil.isTokenValid(jwt, userDetails.getUsername())) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
				log.debug("JWT authenticated: {}", userEmail);
			}
		}

		filterChain.doFilter(request, response);
	}
}
