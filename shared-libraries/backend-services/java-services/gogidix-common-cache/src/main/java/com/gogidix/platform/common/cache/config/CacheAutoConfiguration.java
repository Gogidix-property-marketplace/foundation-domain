package com.gogidix.platform.common.cache.config;

import com.gogidix.platform.common.cache.properties.GogidixCacheProperties;
import com.gogidix.platform.common.cache.service.GogidixCacheService;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Auto-configuration for gogidix-common-cache using Spring Boot's built-in caching
 */
@Slf4j
@AutoConfiguration
@EnableCaching
@EnableConfigurationProperties({CacheProperties.class, GogidixCacheProperties.class})
@ConditionalOnProperty(name = "gogidix.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheAutoConfiguration {

    /**
     * Create Redis cache manager if Redis is available
     */
    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "gogidix.cache.type", havingValue = "redis")
    @Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
                                              GogidixCacheProperties cacheProperties) {
        log.info("Creating Redis cache manager");

        // Default Redis cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(cacheProperties.getDefaultTtl()))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Configure per-cache specifications
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheProperties.getCaches().forEach((name, spec) -> {
            RedisCacheConfiguration config = defaultConfig;

            if (spec.getTtl() > 0) {
                config = config.entryTtl(Duration.ofSeconds(spec.getTtl()));
            }

            cacheConfigurations.put(name, config);
        });

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Create Caffeine cache manager if Caffeine is preferred
     */
    @Bean
    @ConditionalOnClass(Caffeine.class)
    @ConditionalOnProperty(name = "gogidix.cache.type", havingValue = "caffeine", matchIfMissing = true)
    @Primary
    public CaffeineCacheManager caffeineCacheManager(GogidixCacheProperties cacheProperties) {
        log.info("Creating Caffeine cache manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Configure Caffeine builder
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .initialCapacity(cacheProperties.getCaffeine().getInitialCapacity())
                .maximumSize(cacheProperties.getCaffeine().getMaximumSize());

        // Configure expiration policies
        if (cacheProperties.getCaffeine().getExpireAfterWrite() > 0) {
            caffeineBuilder.expireAfterWrite(Duration.ofSeconds(cacheProperties.getCaffeine().getExpireAfterWrite()));
        }
        if (cacheProperties.getCaffeine().getExpireAfterAccess() > 0) {
            caffeineBuilder.expireAfterAccess(Duration.ofSeconds(cacheProperties.getCaffeine().getExpireAfterAccess()));
        }
        if (cacheProperties.getCaffeine().getRefreshAfterWrite() > 0) {
            caffeineBuilder.refreshAfterWrite(Duration.ofSeconds(cacheProperties.getCaffeine().getRefreshAfterWrite()));
        }

        // Configure weak/soft references
        if (cacheProperties.getCaffeine().isWeakKeys()) {
            caffeineBuilder.weakKeys();
        }
        if (cacheProperties.getCaffeine().isWeakValues()) {
            caffeineBuilder.weakValues();
        }
        if (cacheProperties.getCaffeine().isSoftValues()) {
            caffeineBuilder.softValues();
        }

        // Configure statistics recording
        if (cacheProperties.getCaffeine().isRecordStats()) {
            caffeineBuilder.recordStats();
        }

        cacheManager.setCaffeine(caffeineBuilder);
        cacheManager.setAllowNullValues(cacheProperties.isCacheNullValues());

        // Set cache names if configured
        if (!cacheProperties.getCaches().isEmpty()) {
            cacheManager.setCacheNames(cacheProperties.getCaches().keySet());
        }

        return cacheManager;
    }

    /**
     * Create simple cache manager as fallback
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    @Primary
    public ConcurrentMapCacheManager concurrentMapCacheManager(CacheProperties cacheProperties,
                                                             GogidixCacheProperties gogidixCacheProperties) {
        log.info("Creating ConcurrentMap cache manager as fallback");

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // Set cache names if configured
        if (cacheProperties.getCacheNames() != null && !cacheProperties.getCacheNames().isEmpty()) {
            cacheManager.setCacheNames(cacheProperties.getCacheNames());
            log.info("Configured cache names: {}", cacheProperties.getCacheNames());
        } else if (!gogidixCacheProperties.getCaches().isEmpty()) {
            cacheManager.setCacheNames(gogidixCacheProperties.getCaches().keySet());
        }

        // Configure null values
        cacheManager.setAllowNullValues(gogidixCacheProperties.isCacheNullValues());

        return cacheManager;
    }

    /**
     * Create simple cache manager for basic use cases
     */
    @Bean
    @ConditionalOnProperty(name = "gogidix.cache.type", havingValue = "simple")
    public SimpleCacheManager simpleCacheManager() {
        log.info("Creating Simple cache manager");

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<org.springframework.cache.Cache> caches = new ArrayList<>();
        cacheManager.setCaches(caches);

        return cacheManager;
    }

    /**
     * Cache service bean for programmatic cache operations
     */
    @Bean
    @ConditionalOnMissingBean
    public GogidixCacheService gogidixCacheService(CacheManager cacheManager) {
        log.info("Creating Gogidix cache service");
        return new GogidixCacheService(cacheManager);
    }
}