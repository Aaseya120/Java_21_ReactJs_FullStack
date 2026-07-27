package com.demo.user.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.constant.ApiConstants;
import com.demo.common.dto.ApiResponse;
import com.demo.user.dto.UserResponse;
import com.demo.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * User profile controller — all endpoints require authentication.
 */
@RestController
@RequestMapping(ApiConstants.UserApi.BASE)
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/**
	 * GET /api/v1/users/{id} — Retrieve a user profile by ID. Result is cached in
	 * Redis via {@link UserService#getUserById}.
	 */
	@GetMapping(ApiConstants.UserApi.ID)
	public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
	}

	/**
	 * PUT /api/v1/users/{id}/name — Update full name. Only the user themselves or
	 * an ADMIN can update.
	 */
	@PutMapping(ApiConstants.UserApi.NAME)
	@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
	public ResponseEntity<ApiResponse<UserResponse>> updateName(@PathVariable Long id, @RequestParam String fullName) {
		return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, fullName)));
	}
}

