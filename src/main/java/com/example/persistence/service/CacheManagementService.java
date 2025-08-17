package com.example.persistence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing cache operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementService {
    
    private final CacheManager cacheManager;
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        log.info("Clearing all caches");
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", cacheName);
            }
        }
        log.info("All caches cleared successfully");
    }
    
    /**
     * Clear specific cache
     */
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cache '{}' cleared successfully", cacheName);
        } else {
            log.warn("Cache '{}' not found", cacheName);
        }
    }
    
    /**
     * Evict specific key from cache
     */
    public void evictFromCache(String cacheName, Object key) {
        log.info("Evicting key '{}' from cache '{}'", key, cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Key '{}' evicted from cache '{}' successfully", key, cacheName);
        } else {
            log.warn("Cache '{}' not found", cacheName);
        }
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        log.debug("Getting cache statistics");
        Map<String, Object> statistics = new HashMap<>();
        
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheStats = new HashMap<>();
                
                // Try to get Caffeine cache statistics if available
                try {
                    Object nativeCache = cache.getNativeCache();
                    if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                        com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache = 
                            (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
                        
                        com.github.benmanes.caffeine.cache.stats.CacheStats stats = caffeineCache.stats();
                        cacheStats.put("hitCount", stats.hitCount());
                        cacheStats.put("missCount", stats.missCount());
                        cacheStats.put("hitRate", stats.hitRate());
                        cacheStats.put("evictionCount", stats.evictionCount());
                        cacheStats.put("estimatedSize", caffeineCache.estimatedSize());
                    }
                } catch (Exception e) {
                    log.debug("Could not get detailed statistics for cache: {}", cacheName);
                    cacheStats.put("error", "Statistics not available");
                }
                
                statistics.put(cacheName, cacheStats);
            }
        }
        
        return statistics;
    }
    
    /**
     * Get all cache names
     */
    public Collection<String> getCacheNames() {
        return cacheManager.getCacheNames();
    }
    
    /**
     * Check if cache exists
     */
    public boolean cacheExists(String cacheName) {
        return cacheManager.getCache(cacheName) != null;
    }
    
    /**
     * Warm up cache with data
     */
    public void warmUpCache(String cacheName, Map<Object, Object> data) {
        log.info("Warming up cache '{}' with {} entries", cacheName, data.size());
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            for (Map.Entry<Object, Object> entry : data.entrySet()) {
                cache.put(entry.getKey(), entry.getValue());
            }
            log.info("Cache '{}' warmed up successfully", cacheName);
        } else {
            log.warn("Cache '{}' not found for warm up", cacheName);
        }
    }
}