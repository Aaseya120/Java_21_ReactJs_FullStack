package com.demo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Global OpenAPI (Swagger) configuration.
 */
@Configuration
public class OpenApiConfig {

	@Value("${spring.application.name:Microservice}")
	private String applicationName;

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI().info(new Info().title(applicationName + " API").version("1.0.0")
				.description("API Documentation for " + applicationName));
	}
}
