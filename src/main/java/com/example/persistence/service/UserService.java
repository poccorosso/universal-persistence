package com.example.persistence.service;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import com.example.persistence.dto.PageResponse;
import com.example.persistence.entity.User;
import com.example.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * User service class demonstrating how to use the generic Repository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Create single user
     */
    @Transactional
    @CacheEvict(value = {"users", "user-statistics"}, allEntries = true)
    public User createUser(User user) {
        log.info("Creating user: {}", user.getUsername());
        
        // Validate unique constraints
        if (userRepository.existsByUsernameAndDeletedFalse(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        if (userRepository.existsByEmailAndDeletedFalse(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        User saved = userRepository.createOrUpdate(user);
        log.info("Successfully created user with ID: {}", saved.getId());
        return saved;
    }
    
    /**
     * Create multiple users
     */
    @Transactional
    public List<User> createUsers(List<User> users) {
        log.info("Creating {} users", users.size());
        List<User> saved = userRepository.batchInsert(users);
        log.info("Successfully created {} users", saved.size());
        return saved;
    }
    
    /**
     * Update single user
     */
    @Transactional
    @Caching(
        put = @CachePut(value = "users", key = "#user.id"),
        evict = @CacheEvict(value = "user-statistics", allEntries = true)
    )
    public User updateUser(User user) {
        log.info("Updating user ID: {}", user.getId());
        
        Optional<User> existing = userRepository.findById(user.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + user.getId());
        }
        
        User updated = userRepository.createOrUpdate(user);
        log.info("Successfully updated user ID: {}", updated.getId());
        return updated;
    }
    
    /**
     * Update multiple users
     */
    @Transactional
    public List<User> updateUsers(List<User> users) {
        log.info("Updating {} users", users.size());
        List<User> updated = userRepository.createOrUpdateBatch(users);
        log.info("Successfully updated {} users", updated.size());
        return updated;
    }
    
    /**
     * Find user by ID
     */
    @Cacheable(value = "users", key = "#id")
    public Optional<User> findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id)
                .filter(user -> !user.getDeleted());
    }
    
    /**
     * Find user by username
     */
    @Cacheable(value = "users", key = "'username:' + #username")
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsernameAndDeletedFalse(username);
    }
    
    /**
     * Find user by email
     */
    @Cacheable(value = "users", key = "'email:' + #email")
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmailAndDeletedFalse(email);
    }
    
    /**
     * Find all active users
     */
    public List<User> findAllActiveUsers() {
        log.debug("Finding all active users");
        return userRepository.findActiveEntities();
    }
    
    /**
     * Find users by filters
     */
    public List<User> findUsersByFilters(List<FilterCriteria> filters) {
        log.debug("Finding users by filters: {}", filters);
        return userRepository.findByFilters(filters);
    }
    
    /**
     * Find users with pagination
     */
    public PageResponse<User> findUsersWithPage(List<FilterCriteria> filters, 
                                               PageRequest pageRequest) {
        log.debug("Finding users with pagination");
        return userRepository.findByFiltersWithPage(filters, pageRequest);
    }
    
    /**
     * Find users with optional pagination
     */
    public Object findUsersWithOptionalPage(List<FilterCriteria> filters, 
                                           PageRequest pageRequest, 
                                           boolean paginated) {
        log.debug("Finding users with optional pagination: {}", paginated);
        return userRepository.findByFiltersWithOptionalPage(filters, pageRequest, paginated);
    }
    
    /**
     * Execute custom SQL query
     */
    public List<Map<String, Object>> executeCustomQuery(String sql, 
                                                        Map<String, Object> parameters) {
        log.debug("Executing custom query");
        return userRepository.executeNativeQuery(sql, parameters);
    }
    
    /**
     * Get user statistics
     */
    @Cacheable(value = "user-statistics", key = "'all'")
    public Map<String, Object> getUserStatistics() {
        log.debug("Getting user statistics");
        String sql = """
            SELECT 
                COUNT(*) as total_users,
                COUNT(CASE WHEN status = 'ACTIVE' AND deleted = false THEN 1 END) as active_users,
                COUNT(CASE WHEN status = 'INACTIVE' AND deleted = false THEN 1 END) as inactive_users,
                COUNT(CASE WHEN deleted = true THEN 1 END) as deleted_users,
                AVG(CASE WHEN deleted = false THEN age END) as average_age
            FROM users
            """;
        
        List<Map<String, Object>> result = userRepository.executeNativeQuery(sql, null);
        Map<String, Object> stats = result.isEmpty() ? new HashMap<>() : result.get(0);
        log.debug("User statistics: {}", stats);
        return stats;
    }
    
    /**
     * Hard delete user
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "user-statistics", allEntries = true)
    })
    public void deleteUser(Long id) {
        log.info("Hard deleting user ID: {}", id);
        userRepository.deleteById(id);
        log.info("Successfully deleted user ID: {}", id);
    }
    
    /**
     * Soft delete user
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "user-statistics", allEntries = true)
    })
    public void softDeleteUser(Long id) {
        log.info("Soft deleting user ID: {}", id);
        userRepository.softDeleteById(id);
        log.info("Successfully soft deleted user ID: {}", id);
    }
    
    /**
     * Batch delete users
     */
    @Transactional
    public void deleteUsers(List<Long> ids) {
        log.info("Batch deleting {} users", ids.size());
        userRepository.batchDeleteByIds(ids);
        log.info("Successfully deleted {} users", ids.size());
    }
    
    /**
     * Find users by status
     */
    public List<User> findUsersByStatus(User.Status status) {
        log.debug("Finding users by status: {}", status);
        return userRepository.findByStatusAndDeletedFalse(status);
    }
    
    /**
     * Find users by age range
     */
    public List<User> findUsersByAgeRange(Integer minAge, Integer maxAge) {
        log.debug("Finding users by age range: {} - {}", minAge, maxAge);
        return userRepository.findByAgeBetweenAndDeletedFalse(minAge, maxAge);
    }
}