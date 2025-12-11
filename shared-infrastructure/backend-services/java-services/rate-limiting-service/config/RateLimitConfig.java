package com.gogidix.infrastructure.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configuration for Rate Limiting Service.
 *
 * Configures Redis, caching, and rate limiting specific beans.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Configuration
public class RateLimitConfig {

    /**
     * Redis configuration properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    /**
     * Redis connection factory with optimized settings for rate limiting.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties properties) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                properties.getHost(),
                properties.getPort()
        );

        // Configure connection pool for high performance
        factory.setShareNativeConnection(false);

        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            factory.setPassword(properties.getPassword());
        }

        if (properties.getDatabase() != null) {
            factory.setDatabase(properties.getDatabase());
        }

        return factory;
    }

    /**
     * Redis template optimized for rate limiting operations.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys (more efficient for rate limit keys)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis properties holder.
     */
    public static class RedisProperties {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private Integer database;
        private int timeout = 2000;
        private Pool pool = new Pool();

        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Integer getDatabase() { return database; }
        public void setDatabase(Integer database) { this.database = database; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public Pool getPool() { return pool; }
        public void setPool(Pool pool) { this.pool = pool; }

        public static class Pool {
            private int maxActive = 8;
            private int maxIdle = 8;
            private int minIdle = 0;
            private long maxWait = -1;

            // Getters and Setters
            public int getMaxActive() { return maxActive; }
            public void setMaxActive(int maxActive) { this.maxActive = maxActive; }

            public int getMaxIdle() { return maxIdle; }
            public void setMaxIdle(int maxIdle) { this.maxIdle = maxIdle; }

            public int getMinIdle() { return minIdle; }
            public void setMinIdle(int minIdle) { this.minIdle = minIdle; }

            public long getMaxWait() { return maxWait; }
            public void setMaxWait(long maxWait) { this.maxWait = maxWait; }
        }
    }
}