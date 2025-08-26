package com.example.persistence.controller;

import com.example.persistence.dto.BaseResponse;
import com.example.persistence.dto.UserProjection;
import com.example.persistence.entity.DataSourceEntity;
import com.example.persistence.entity.User;
import com.example.persistence.service.LinqExampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller demonstrating LINQ-style query usage
 */
@RestController
@RequestMapping("/api/linq-examples")
@RequiredArgsConstructor
@Slf4j
public class LinqExampleController {
    
    private final LinqExampleService linqExampleService;
    
    /**
     * Get first user by uid
     */
    @GetMapping("/first-user/{uid}")
    public ResponseEntity<User> getFirstUserByUid(@PathVariable String uid) {
        log.info("API: Getting first user by uid: {}", uid);
        User user = linqExampleService.findFirstUserByUid(uid);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
    
    /**
     * Check if any users exist with specific email domain
     */
    @GetMapping("/any-users-with-domain/{domain}")
    public ResponseEntity<Map<String, Object>> anyUsersWithEmailDomain(@PathVariable String domain) {
        log.info("API: Checking if any users exist with email domain: {}", domain);
        boolean exists = linqExampleService.anyUsersWithEmailDomain("@" + domain);
        return ResponseEntity.ok(Map.of("exists", exists, "domain", "@" + domain));
    }

    /**
     * Get active users with pagination
     */
    @GetMapping("/active-users-paginated")
    public ResponseEntity<Page<User>> getActiveUsersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("API: Getting active users with pagination: page={}, size={}", page, size);
        Page<User> users = linqExampleService.getActiveUsersWithPagination(page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Find users with names starting with specific letter
     */
    @GetMapping("/users-by-name-starting-with/{letter}")
    public ResponseEntity<List<User>> findUsersByNameStartingWith(@PathVariable String letter) {
        log.info("API: Finding users with names starting with: {}", letter);
        List<User> users = linqExampleService.findUsersByNameStartingWith(letter);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user basic info using Select
     */
    @GetMapping("/user-basic-info")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getUserBasicInfo() {
        log.info("API: Getting user basic info using LINQ Select");
        try {
            List<Map<String, Object>> users = linqExampleService.getUserBasicInfo();
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Retrieved basic info for %d users", users.size())));
        } catch (Exception e) {
            log.error("Error getting user basic info", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get user basic info"));
        }
    }
    
    /**
     * Get user projections using Select
     */
    @GetMapping("/user-projections")
    public ResponseEntity<BaseResponse<List<UserProjection>>> getUserProjections() {
        log.info("API: Getting user projections using LINQ Select");
        try {
            List<UserProjection> users = linqExampleService.getUserProjections();
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Retrieved %d user projections", users.size())));
        } catch (Exception e) {
            log.error("Error getting user projections", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get user projections"));
        }
    }
    
    /**
     * Get all uids using Select
     */
    @GetMapping("/uids")
    public ResponseEntity<List<String>> getAllUids() {
        log.info("API: Getting all uids using LINQ Select");
        List<String> usernames = linqExampleService.getAllUids();
        return ResponseEntity.ok(usernames);
    }
    
    /**
     * Get user basic info with pagination using Select
     */
    @GetMapping("/user-basic-info-paginated")
    public ResponseEntity<Page<Map<String, Object>>> getUserBasicInfoWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("API: Getting user basic info with pagination using LINQ Select");
        Page<Map<String, Object>> users = linqExampleService.getUserBasicInfoWithPagination(page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Find active data sources by type
     */
    @GetMapping("/active-datasources/{type}")
    public ResponseEntity<BaseResponse<List<DataSourceEntity>>> findActiveDataSourcesByType(@PathVariable DataSourceEntity.DatabaseType type) {
        log.info("API: Finding active data sources by type: {}", type);
        try {
            List<DataSourceEntity> dataSources = linqExampleService.findActiveDataSourcesByType(type);
            return ResponseEntity.ok(BaseResponse.success(dataSources,
                    String.format("Found %d active data sources of type %s", dataSources.size(), type)));
        } catch (Exception e) {
            log.error("Error finding active data sources by type", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to find data sources"));
        }
    }
}
