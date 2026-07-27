package com.demo.user.entity;

/**
 * Sealed interface representing user account roles. Using a sealed interface +
 * records demonstrates Java 21's sealed type system.
 */
public enum Role {
	USER, ADMIN, MODERATOR
}
