package com.example.persistence.controller;

import com.example.persistence.dto.BaseResponse;
import com.example.persistence.service.CacheManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Controller for cache management operations
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheManagementController {
    
    private final CacheManagementService cacheManagementService;
    
    /**
     * Get all cache names
     */
    @GetMapping("/names")
    public ResponseEntity<BaseResponse<Collection<String>>> getCacheNames() {
        log.info("Getting all cache names");
        try {
            Collection<String> cacheNames = cacheManagementService.getCacheNames();
            return ResponseEntity.ok(BaseResponse.success(cacheNames, 
                String.format("Found %d cache(s)", cacheNames.size())));
        } catch (Exception e) {
            log.error("Error getting cache names", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get cache names"));
        }
    }
    
    /**
     * Get cache statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getCacheStatistics() {
        log.info("Getting cache statistics");
        try {
            Map<String, Object> statistics = cacheManagementService.getCacheStatistics();
            return ResponseEntity.ok(BaseResponse.success(statistics, "Cache statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get cache statistics"));
        }
    }
    
    /**
     * Clear all caches
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<BaseResponse<Void>> clearAllCaches() {
        log.info("Clearing all caches via API");
        try {
            cacheManagementService.clearAllCaches();
            return ResponseEntity.ok(BaseResponse.success("All caches cleared successfully"));
        } catch (Exception e) {
            log.error("Error clearing all caches", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to clear all caches"));
        }
    }
    
    /**
     * Clear specific cache
     */
    @DeleteMapping("/clear/{cacheName}")
    public ResponseEntity<BaseResponse<Void>> clearCache(@PathVariable String cacheName) {
        log.info("Clearing cache '{}' via API", cacheName);
        try {
            cacheManagementService.clearCache(cacheName);
            return ResponseEntity.ok(BaseResponse.success("Cache '" + cacheName + "' cleared successfully"));
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to clear cache"));
        }
    }
    
    /**
     * Evict specific key from cache
     */
    @DeleteMapping("/evict/{cacheName}/{key}")
    public ResponseEntity<BaseResponse<Void>> evictFromCache(@PathVariable String cacheName, 
                                                             @PathVariable String key) {
        log.info("Evicting key '{}' from cache '{}' via API", key, cacheName);
        try {
            cacheManagementService.evictFromCache(cacheName, key);
            return ResponseEntity.ok(BaseResponse.success("Key '" + key + "' evicted from cache '" + cacheName + "' successfully"));
        } catch (Exception e) {
            log.error("Error evicting from cache", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to evict from cache"));
        }
    }

    /**
     * Check if cache exists
     */
    @GetMapping("/exists/{cacheName}")
    public ResponseEntity<BaseResponse<Map<String, Object>>> cacheExists(@PathVariable String cacheName) {
        log.debug("Checking if cache '{}' exists", cacheName);
        try {
            boolean exists = cacheManagementService.cacheExists(cacheName);
            Map<String, Object> result = Map.of("exists", exists, "cacheName", cacheName);
            return ResponseEntity.ok(BaseResponse.success(result, "Cache existence checked"));
        } catch (Exception e) {
            log.error("Error checking cache existence", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to check cache existence"));
        }
    }
    
    /**
     * Warm up cache with sample data
     */
    @PostMapping("/warmup/{cacheName}")
    public ResponseEntity<BaseResponse<Void>> warmUpCache(@PathVariable String cacheName,
                                                          @RequestBody Map<Object, Object> data) {
        log.info("Warming up cache '{}' with {} entries via API", cacheName, data.size());
        try {
            cacheManagementService.warmUpCache(cacheName, data);
            return ResponseEntity.ok(BaseResponse.success("Cache '" + cacheName + "' warmed up successfully with " + data.size() + " entries"));
        } catch (Exception e) {
            log.error("Error warming up cache", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to warm up cache"));
        }
    }
}