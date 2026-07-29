package com.demo.common.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Shared minimal Redis cache configuration.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	@org.springframework.context.annotation.Primary
	@org.springframework.beans.factory.annotation.Qualifier("defaultCacheConfig")
	public RedisCacheConfiguration cacheConfiguration() {
		com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator ptv = com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
				.allowIfBaseType(Object.class)
				.build();

		com.fasterxml.jackson.databind.ObjectMapper objectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
				.addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
				.activateDefaultTyping(ptv, com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL, com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY)
				.build();

		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15)).serializeValuesWith(
				RedisSerializationContext.SerializationPair.fromSerializer(serializer))
				.disableCachingNullValues();
	}

	@Bean
	@org.springframework.beans.factory.annotation.Qualifier("shortLivedCacheConfig")
	public RedisCacheConfiguration shortLivedCacheConfig() {
		return cacheConfiguration().entryTtl(Duration.ofMinutes(1));
	}
}
