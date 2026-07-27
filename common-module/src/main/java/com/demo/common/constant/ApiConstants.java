package com.demo.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Global API URI Constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiConstants {

	public static final String BASE_API = "/api/v1";
	public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class OrderApi {
		public static final String BASE = BASE_API + "/orders";
		public static final String ID = "/{id}";
		public static final String USER_ID = "/user/{userId}";
		public static final String STATUS = "/{id}/status";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class ProductApi {
		public static final String BASE = BASE_API + "/products";
		public static final String ID = "/{id}";
		public static final String STOCK_DEDUCT = "/{id}/stock/deduct";
		public static final String STOCK_ADD = "/{id}/stock/add";
		public static final String CATEGORY = "/category/{category}";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class UserApi {
		public static final String BASE = BASE_API + "/users";
		public static final String ID = "/{id}";
		public static final String NAME = "/{id}/name";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class AuthApi {
		public static final String BASE = BASE_API + "/auth";
		public static final String REGISTER = "/register";
		public static final String LOGIN = "/login";
		public static final String REFRESH = "/refresh";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class NotificationApi {
		public static final String BASE = BASE_API + "/notifications";
		public static final String STREAM = "/stream/{userId}";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class AggregatorApi {
		public static final String BASE = BASE_API + "/aggregator";
		public static final String ORDER_DETAILS = "/order-details/{orderId}";
	}
}
