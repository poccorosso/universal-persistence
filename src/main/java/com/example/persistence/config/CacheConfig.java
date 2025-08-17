package com.example.persistence.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Cache configuration using Caffeine
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Configure Caffeine cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure default cache settings
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .expireAfterWrite(Duration.ofHours(2))
            .recordStats());
        
        // Define cache names
        cacheManager.setCacheNames(List.of("users", "datasources", "query-results", "user-statistics"));
        
        return cacheManager;
    }
    
    /**
     * Configure specific cache for users
     */
    @Bean("userCacheManager")
    public CacheManager userCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(15))
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats());
        return cacheManager;
    }
    
    /**
     * Configure cache for query results
     */
    @Bean("queryResultCacheManager")
    public CacheManager queryResultCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("query-results");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(200)
            .maximumSize(2000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats());
        return cacheManager;
    }
    
    /**
     * Configure cache for datasources
     */
    @Bean("datasourceCacheManager")
    public CacheManager datasourceCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("datasources");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(20)
            .maximumSize(100)
            .expireAfterAccess(Duration.ofHours(1))
            .expireAfterWrite(Duration.ofHours(4))
            .recordStats());
        return cacheManager;
    }
}