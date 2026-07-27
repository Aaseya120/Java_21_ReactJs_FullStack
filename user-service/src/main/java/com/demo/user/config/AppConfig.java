package com.demo.user.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableCaching
@EnableAsync
@EnableJpaRepositories(basePackages = "com.demo")
@EntityScan(basePackages = "com.demo")
@ComponentScan(basePackages = "com.demo")
public class AppConfig {
}
