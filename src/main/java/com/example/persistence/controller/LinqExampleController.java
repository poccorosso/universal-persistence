package com.example.persistence.controller;

import com.example.persistence.dto.BaseResponse;
import com.example.persistence.dto.UserProjection;
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
     * Get active users older than 25
     */
    @GetMapping("/active-users-older-than-25")
    public ResponseEntity<BaseResponse<List<User>>> getActiveUsersOlderThan25() {
        log.info("API: Getting active users older than 25");
        try {
            List<User> users = linqExampleService.findActiveUsersOlderThan25();
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Found %d active users older than 25", users.size())));
        } catch (Exception e) {
            log.error("Error getting active users older than 25", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get users"));
        }
    }
    
    /**
     * Get users with complex conditions
     */
    @GetMapping("/users-complex-conditions")
    public ResponseEntity<BaseResponse<List<User>>> getUsersWithComplexConditions() {
        log.info("API: Getting users with complex conditions");
        try {
            List<User> users = linqExampleService.findUsersWithComplexConditions();
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Found %d users matching complex conditions", users.size())));
        } catch (Exception e) {
            log.error("Error getting users with complex conditions", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get users"));
        }
    }
    
    /**
     * Get first user by username
     */
    @GetMapping("/first-user/{username}")
    public ResponseEntity<User> getFirstUserByUsername(@PathVariable String username) {
        log.info("API: Getting first user by username: {}", username);
        User user = linqExampleService.findFirstUserByUsername(username);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
    
    /**
     * Count active users
     */
    @GetMapping("/count-active-users")
    public ResponseEntity<Map<String, Long>> countActiveUsers() {
        log.info("API: Counting active users");
        long count = linqExampleService.countActiveUsers();
        return ResponseEntity.ok(Map.of("activeUserCount", count));
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
     * Get top 5 youngest users
     */
    @GetMapping("/top-5-youngest")
    public ResponseEntity<List<User>> getTop5YoungestUsers() {
        log.info("API: Getting top 5 youngest users");
        List<User> users = linqExampleService.getTop5YoungestUsers();
        return ResponseEntity.ok(users);
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
     * Find users by age range
     */
    @GetMapping("/users-by-age-range")
    public ResponseEntity<List<User>> findUsersByAgeRange(
            @RequestParam int minAge,
            @RequestParam int maxAge) {
        log.info("API: Finding users by age range: {} - {}", minAge, maxAge);
        List<User> users = linqExampleService.findUsersByAgeRange(minAge, maxAge);
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
     * Find users by multiple statuses
     */
    @PostMapping("/users-by-statuses")
    public ResponseEntity<List<User>> findUsersByStatuses(@RequestBody List<User.Status> statuses) {
        log.info("API: Finding users by statuses: {}", statuses);
        List<User> users = linqExampleService.findUsersByStatuses(statuses);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Check if all users have email addresses
     */
    @GetMapping("/all-users-have-email")
    public ResponseEntity<Map<String, Boolean>> allUsersHaveEmail() {
        log.info("API: Checking if all users have email addresses");
        boolean allHaveEmail = linqExampleService.allUsersHaveEmail();
        return ResponseEntity.ok(Map.of("allUsersHaveEmail", allHaveEmail));
    }
    
    /**
     * Get user count by status
     */
    @GetMapping("/user-count-by-status")
    public ResponseEntity<Map<Object, Long>> getUserCountByStatus() {
        log.info("API: Getting user count by status");
        Map<Object, Long> counts = linqExampleService.getUserCountByStatus();
        return ResponseEntity.ok(counts);
    }
    
    /**
     * Find adult users with complex sorting
     */
    @GetMapping("/adult-users-complex-sorting")
    public ResponseEntity<List<User>> findAdultUsersWithComplexSorting() {
        log.info("API: Finding adult users with complex sorting");
        List<User> users = linqExampleService.findAdultUsersWithComplexSorting();
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
     * Get all usernames using Select
     */
    @GetMapping("/usernames")
    public ResponseEntity<List<String>> getAllUsernames() {
        log.info("API: Getting all usernames using LINQ Select");
        List<String> usernames = linqExampleService.getAllUsernames();
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
}