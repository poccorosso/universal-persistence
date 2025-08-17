package com.example.persistence.controller;

import com.example.persistence.dto.BaseResponse;
import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import com.example.persistence.dto.PageResponse;
import com.example.persistence.entity.User;
import com.example.persistence.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User controller demonstrating API usage
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * Create single user
     */
    @PostMapping
    public ResponseEntity<BaseResponse<User>> createUser(@RequestBody User user) {
        log.info("Creating user via API: {}", user.getUsername());
        try {
            User created = userService.createUser(user);
            return ResponseEntity.ok(BaseResponse.success(created, "User created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to create user"));
        }
    }
    
    /**
     * Create multiple users
     */
    @PostMapping("/batch")
    public ResponseEntity<BaseResponse<List<User>>> createUsers(@RequestBody List<User> users) {
        log.info("Creating {} users via API", users.size());
        try {
            List<User> created = userService.createUsers(users);
            return ResponseEntity.ok(BaseResponse.success(created, 
                String.format("Successfully created %d users", created.size())));
        } catch (Exception e) {
            log.error("Error creating users", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to create users"));
        }
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<User>> getUser(@PathVariable Long id) {
        log.debug("Getting user by ID: {}", id);
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(BaseResponse.success(user, "User found")))
                .orElse(ResponseEntity.ok(BaseResponse.notFound("User not found with ID: " + id)));
    }
    
    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<BaseResponse<User>> getUserByUsername(@PathVariable String username) {
        log.debug("Getting user by username: {}", username);
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(BaseResponse.success(user, "User found")))
                .orElse(ResponseEntity.ok(BaseResponse.notFound("User not found with username: " + username)));
    }
    
    /**
     * Get all active users
     */
    @GetMapping("/active")
    public ResponseEntity<BaseResponse<List<User>>> getActiveUsers() {
        log.debug("Getting all active users");
        try {
            List<User> users = userService.findAllActiveUsers();
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Found %d active users", users.size())));
        } catch (Exception e) {
            log.error("Error getting active users", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get active users"));
        }
    }
    
    /**
     * Search users with filters
     */
    @PostMapping("/search")
    public ResponseEntity<BaseResponse<List<User>>> searchUsers(@RequestBody List<FilterCriteria> filters) {
        log.debug("Searching users with filters");
        try {
            List<User> users = userService.findUsersByFilters(filters);
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Found %d users matching filters", users.size())));
        } catch (Exception e) {
            log.error("Error searching users", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to search users"));
        }
    }
    
    /**
     * Search users with pagination
     */
    @PostMapping("/search/page")
    public ResponseEntity<BaseResponse<PageResponse<User>>> searchUsersWithPage(
            @RequestBody Map<String, Object> request) {
        log.debug("Searching users with pagination");
        try {
            @SuppressWarnings("unchecked")
            List<FilterCriteria> filters = (List<FilterCriteria>) request.get("filters");
            PageRequest pageRequest = (PageRequest) request.get("pageRequest");
            
            PageResponse<User> result = userService.findUsersWithPage(filters, pageRequest);
            return ResponseEntity.ok(BaseResponse.success(result, "Users retrieved successfully"));
        } catch (Exception e) {
            log.error("Error searching users with pagination", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to search users"));
        }
    }
    
    /**
     * Search users with optional pagination
     */
    @PostMapping("/search/optional-page")
    public ResponseEntity<BaseResponse<Object>> searchUsersWithOptionalPage(
            @RequestBody Map<String, Object> request) {
        log.debug("Searching users with optional pagination");
        try {
            @SuppressWarnings("unchecked")
            List<FilterCriteria> filters = (List<FilterCriteria>) request.get("filters");
            PageRequest pageRequest = (PageRequest) request.get("pageRequest");
            Boolean paginated = (Boolean) request.getOrDefault("paginated", false);
            
            Object result = userService.findUsersWithOptionalPage(filters, pageRequest, paginated);
            return ResponseEntity.ok(BaseResponse.success(result, "Users retrieved successfully"));
        } catch (Exception e) {
            log.error("Error searching users with optional pagination", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to search users"));
        }
    }
    
    /**
     * Execute custom SQL query
     */
    @PostMapping("/query")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> executeQuery(
            @RequestBody Map<String, Object> request) {
        log.info("Executing custom query via API");
        
        String sql = (String) request.get("sql");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
        
        try {
            List<Map<String, Object>> results = userService.executeCustomQuery(sql, parameters);
            Map<String, Object> metadata = Map.of("rowCount", results.size(), "sql", sql);
            
            return ResponseEntity.ok(BaseResponse.successWithMetadata(results, 
                "Query executed successfully", metadata));
            
        } catch (SecurityException e) {
            log.error("Security error executing query: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(BaseResponse.badRequest("SQL query rejected: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error executing custom query: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to execute query"));
        }
    }
    
    /**
     * Get user statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getUserStatistics() {
        log.debug("Getting user statistics");
        try {
            Map<String, Object> stats = userService.getUserStatistics();
            return ResponseEntity.ok(BaseResponse.success(stats, "Statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get statistics"));
        }
    }
    
    /**
     * Update user
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<User>> updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("Updating user ID: {}", id);
        try {
            user.setId(id);
            User updated = userService.updateUser(user);
            return ResponseEntity.ok(BaseResponse.success(updated, "User updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to update user"));
        }
    }
    
    /**
     * Update multiple users
     */
    @PutMapping("/batch")
    public ResponseEntity<BaseResponse<List<User>>> updateUsers(@RequestBody List<User> users) {
        log.info("Updating {} users via API", users.size());
        try {
            List<User> updated = userService.updateUsers(users);
            return ResponseEntity.ok(BaseResponse.success(updated, 
                String.format("Successfully updated %d users", updated.size())));
        } catch (Exception e) {
            log.error("Error updating users", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to update users"));
        }
    }
    
    /**
     * Hard delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("Hard deleting user ID: {}", id);
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(BaseResponse.success("User deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to delete user"));
        }
    }
    
    /**
     * Soft delete user
     */
    @PostMapping("/{id}/soft-delete")
    public ResponseEntity<BaseResponse<Void>> softDeleteUser(@PathVariable Long id) {
        log.info("Soft deleting user ID: {}", id);
        try {
            userService.softDeleteUser(id);
            return ResponseEntity.ok(BaseResponse.success("User soft deleted successfully"));
        } catch (Exception e) {
            log.error("Error soft deleting user", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to soft delete user"));
        }
    }
    
    /**
     * Batch delete users
     */
    @DeleteMapping("/batch")
    public ResponseEntity<BaseResponse<Void>> deleteUsers(@RequestBody List<Long> ids) {
        log.info("Batch deleting {} users", ids.size());
        try {
            userService.deleteUsers(ids);
            return ResponseEntity.ok(BaseResponse.success(
                String.format("Successfully deleted %d users", ids.size())));
        } catch (Exception e) {
            log.error("Error batch deleting users", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to delete users"));
        }
    }
    
    /**
     * Get users by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<BaseResponse<List<User>>> getUsersByStatus(@PathVariable User.Status status) {
        log.debug("Getting users by status: {}", status);
        try {
            List<User> users = userService.findUsersByStatus(status);
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Found %d users with status %s", users.size(), status)));
        } catch (Exception e) {
            log.error("Error getting users by status", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get users by status"));
        }
    }
    
    /**
     * Get users by age range
     */
    @GetMapping("/age/{minAge}/{maxAge}")
    public ResponseEntity<BaseResponse<List<User>>> getUsersByAgeRange(@PathVariable Integer minAge, 
                                                        @PathVariable Integer maxAge) {
        log.debug("Getting users by age range: {} - {}", minAge, maxAge);
        try {
            List<User> users = userService.findUsersByAgeRange(minAge, maxAge);
            return ResponseEntity.ok(BaseResponse.success(users, 
                String.format("Found %d users in age range %d-%d", users.size(), minAge, maxAge)));
        } catch (Exception e) {
            log.error("Error getting users by age range", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get users by age range"));
        }
    }
}