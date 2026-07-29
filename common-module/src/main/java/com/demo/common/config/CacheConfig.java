package com.demo.common.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared minimal Redis cache configuration.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	@Primary
	@Qualifier("defaultCacheConfig")
	public RedisCacheConfiguration cacheConfiguration() {
		BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
				.builder()
				.allowIfBaseType(Object.class)
				.build();

		DefaultTypeResolverBuilder typer = new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, ptv) {
			@Override
			public boolean useForType(JavaType t) {
				if (t.isRecordType()) {
					return true;
				}
				return super.useForType(t);
			}
		};
		typer.init(JsonTypeInfo.Id.CLASS, null);
		typer.inclusion(JsonTypeInfo.As.PROPERTY);

		com.fasterxml.jackson.databind.ObjectMapper objectMapper = JsonMapper
				.builder()
				.addModule(new JavaTimeModule())
				.build();
		objectMapper.setDefaultTyping(typer);

		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15)).serializeValuesWith(
				RedisSerializationContext.SerializationPair.fromSerializer(serializer))
				.disableCachingNullValues();
	}

	@Bean
	@Qualifier("shortLivedCacheConfig")
	public RedisCacheConfiguration shortLivedCacheConfig() {
		return cacheConfiguration().entryTtl(Duration.ofMinutes(1));
	}
}
