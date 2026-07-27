package com.demo.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.constant.ApiConstants;
import com.demo.common.dto.ApiResponse;
import com.demo.user.dto.JwtResponse;
import com.demo.user.dto.LoginRequest;
import com.demo.user.dto.RegisterRequest;
import com.demo.user.dto.TokenRefreshRequest;
import com.demo.user.dto.LogoutRequest;
import com.demo.user.dto.UserResponse;
import com.demo.user.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Authentication controller — handles registration and login (public
 * endpoints).
 */
@RestController
@RequestMapping(ApiConstants.AuthApi.BASE)
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/**
	 * POST /api/v1/auth/register — Register a new user account.
	 */
	@PostMapping(ApiConstants.AuthApi.REGISTER)
	public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest request) {
		JwtResponse jwt = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(jwt));
	}

	/**
	 * POST /api/v1/auth/login — Authenticate and receive a JWT.
	 */
	@PostMapping(ApiConstants.AuthApi.LOGIN)
	public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
		JwtResponse jwt = authService.login(request);
		return ResponseEntity.ok(ApiResponse.success(jwt));
	}

	/**
	 * POST /api/v1/auth/refresh — Refresh an expired access token using a refresh
	 * token.
	 */
	@PostMapping(ApiConstants.AuthApi.REFRESH)
	public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
		JwtResponse jwt = authService.refreshToken(request.refreshToken());
		return ResponseEntity.ok(ApiResponse.success(jwt));
	}
	/**
	 * POST /api/v1/auth/logout — Logout by invalidating the refresh token.
	 */
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
	}

	/**
	 * GET /api/v1/auth/recover-id — Recover User ID by email or mobile number.
	 */
	@org.springframework.web.bind.annotation.GetMapping("/recover-id")
	public ResponseEntity<ApiResponse<UserResponse>> recoverUserId(
			@org.springframework.web.bind.annotation.RequestParam("contact") String contact) {
		return ResponseEntity.ok(ApiResponse.success(authService.recoverUserId(contact)));
	}
}
