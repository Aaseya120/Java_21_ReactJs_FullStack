package com.demo.user.service;


import com.demo.user.dto.UserResponse;

public interface UserService {
	UserResponse getUserById(Long id);

	UserResponse updateUser(Long id, String newFullName);
}

