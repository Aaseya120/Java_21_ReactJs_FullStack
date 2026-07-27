package com.demo.user.service;

import com.demo.user.dto.JwtResponse;
import com.demo.user.dto.LoginRequest;
import com.demo.user.dto.RegisterRequest;
import com.demo.user.dto.UserResponse;

public interface AuthService {
	JwtResponse register(RegisterRequest request);

	JwtResponse login(LoginRequest request);

	JwtResponse refreshToken(String requestRefreshToken);

	void logout(String refreshToken);

	UserResponse recoverUserId(String contact);
}
