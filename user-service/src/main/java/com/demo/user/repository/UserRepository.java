package com.demo.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.user.entity.User;

/**
 * Spring Data JPA repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	/** Find a user by email (used in authentication). */
	Optional<User> findByEmail(String email);

	/** Check if an email is already registered. */
	boolean existsByEmail(String email);

	/** Find a user by email or mobile number (for User ID recovery). */
	Optional<User> findByEmailOrMobileNumber(String email, String mobileNumber);
}

