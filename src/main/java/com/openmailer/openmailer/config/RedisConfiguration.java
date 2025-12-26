package com.openmailer.openmailer.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching.
 * Defines cache managers with different TTL settings for different cache regions.
 */
@Configuration
@EnableCaching
public class RedisConfiguration {

    /**
     * Configure RedisTemplate for general Redis operations.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure CacheManager with different TTL for different cache regions.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration (10 minutes TTL)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()))
                .disableCachingNullValues();

        // Specific cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // User sessions cache: 15 minutes
        cacheConfigurations.put("userSessions", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // User cache: 30 minutes (for frequently accessed user data)
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Segment counts cache: 10 minutes
        cacheConfigurations.put("segmentCounts", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Domain verification status cache: 1 hour
        cacheConfigurations.put("domainVerification", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Campaign statistics cache: 5 minutes
        cacheConfigurations.put("campaignStats", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Email provider cache: 1 hour
        cacheConfigurations.put("providers", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Contact list stats: 15 minutes
        cacheConfigurations.put("listStats", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Template cache: 30 minutes
        cacheConfigurations.put("templates", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Rate limit counters: 1 minute
        cacheConfigurations.put("rateLimits", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Create a JSON serializer with proper configuration for Java types.
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register JavaTimeModule for LocalDateTime, ZonedDateTime, etc.
        objectMapper.registerModule(new JavaTimeModule());

        // Enable default typing for polymorphic deserialization
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
