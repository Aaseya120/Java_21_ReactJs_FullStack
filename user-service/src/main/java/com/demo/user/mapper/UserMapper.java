package com.demo.user.mapper;

import org.mapstruct.Mapper;

import com.demo.user.dto.UserResponse;
import com.demo.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
	UserResponse toResponse(User user);
}
