package com.example.persistence.service;

import com.example.persistence.dto.UserProjection;
import com.example.persistence.entity.User;
import com.example.persistence.repository.UserRepository;
import com.example.persistence.util.LinqQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service demonstrating LINQ-style query usage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinqExampleService {
    
    private final UserRepository userRepository;
    
    /**
     * Example: Find active users older than 25, ordered by age
     * Equivalent to: users.Where(u => u.Status == ACTIVE && u.Age > 25).OrderBy(u => u.Age).ToList()
     */
    public List<User> findActiveUsersOlderThan25() {
        log.info("Finding active users older than 25 using LINQ");
        
        return userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .where("age", LinqQueryBuilder.WhereOperator.GREATER_THAN, 25)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .toList();
    }
    
    /**
     * Example: Find users with complex conditions
     * Equivalent to: users.Where(u => (u.Age > 30 || u.Status == INACTIVE) && u.Username.Contains("john")).ToList()
     */
    public List<User> findUsersWithComplexConditions() {
        log.info("Finding users with complex conditions using LINQ");
        
        return userRepository.linq()
            .where(w -> w.or(
                w.field("age").gt(30),
                w.field("status").eq(User.Status.INACTIVE)
            ))
            .where("username", LinqQueryBuilder.WhereOperator.CONTAINS, "john")
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
    }
    
    /**
     * Example: Get first user by username
     * Equivalent to: users.Where(u => u.Username == username).FirstOrDefault()
     */
    public User findFirstUserByUsername(String username) {
        log.info("Finding first user by username: {}", username);
        
        return userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.EQUALS, username)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .firstOrDefault();
    }
    
    /**
     * Example: Count active users
     * Equivalent to: users.Where(u => u.Status == ACTIVE).Count()
     */
    public long countActiveUsers() {
        log.info("Counting active users using LINQ");
        
        return userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .count();
    }
    
    /**
     * Example: Check if any users exist with specific email domain
     * Equivalent to: users.Any(u => u.Email.EndsWith("@example.com"))
     */
    public boolean anyUsersWithEmailDomain(String domain) {
        log.info("Checking if any users exist with email domain: {}", domain);
        
        return userRepository.linq()
            .where("email", LinqQueryBuilder.WhereOperator.ENDS_WITH, domain)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .any();
    }
    
    /**
     * Example: Get top 5 youngest users
     * Equivalent to: users.Where(u => u.Age != null).OrderBy(u => u.Age).Take(5).ToList()
     */
    public List<User> getTop5YoungestUsers() {
        log.info("Getting top 5 youngest users using LINQ");
        
        return userRepository.linq()
            .where("age", LinqQueryBuilder.WhereOperator.IS_NOT_NULL, null)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .take(5)
            .toList();
    }
    
    /**
     * Example: Get users with pagination
     * Equivalent to: users.Where(u => u.Status == ACTIVE).OrderByDescending(u => u.CreatedAt).Skip(page * size).Take(size)
     */
    public Page<User> getActiveUsersWithPagination(int page, int size) {
        log.info("Getting active users with pagination: page={}, size={}", page, size);
        
        PageRequest pageRequest = PageRequest.of(page, size);
        
        return userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderByDescending("createdAt")
            .toPage(pageRequest);
    }
    
    /**
     * Example: Find users by age range
     * Equivalent to: users.Where(u => u.Age >= minAge && u.Age <= maxAge).ToList()
     */
    public List<User> findUsersByAgeRange(int minAge, int maxAge) {
        log.info("Finding users by age range: {} - {}", minAge, maxAge);
        
        return userRepository.linq()
            .where("age", LinqQueryBuilder.WhereOperator.GREATER_THAN_OR_EQUAL, minAge)
            .where("age", LinqQueryBuilder.WhereOperator.LESS_THAN_OR_EQUAL, maxAge)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .toList();
    }
    
    /**
     * Example: Find users with names starting with specific letter
     * Equivalent to: users.Where(u => u.FullName.StartsWith(letter)).OrderBy(u => u.FullName).ToList()
     */
    public List<User> findUsersByNameStartingWith(String letter) {
        log.info("Finding users with names starting with: {}", letter);
        
        return userRepository.linq()
            .where("fullName", LinqQueryBuilder.WhereOperator.STARTS_WITH, letter)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("fullName")
            .toList();
    }
    
    /**
     * Example: Find users by multiple statuses
     * Equivalent to: users.Where(u => statuses.Contains(u.Status)).ToList()
     */
    public List<User> findUsersByStatuses(List<User.Status> statuses) {
        log.info("Finding users by statuses: {}", statuses);
        
        return userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.IN, statuses)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("username")
            .toList();
    }
    
    /**
     * Example: Check if all users have email addresses
     * Equivalent to: users.All(u => u.Email != null && !u.Email.isEmpty())
     */
    public boolean allUsersHaveEmail() {
        log.info("Checking if all users have email addresses");
        
        return userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .all("email", LinqQueryBuilder.WhereOperator.IS_NOT_NULL, null);
    }
    
    /**
     * Example: Get users grouped by status (using traditional approach since groupBy is complex)
     */
    public Map<Object, Long> getUserCountByStatus() {
        log.info("Getting user count by status using LINQ GroupBy");
        
        return userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .groupBy("status")
            .count();
    }
    
    /**
     * Example: Select specific fields as Map
     * Equivalent to: users.Select(u => new { u.Username, u.Email, u.Age }).ToList()
     */
    public List<Map<String, Object>> getUserBasicInfo() {
        log.info("Getting user basic info using LINQ Select");
        
        return userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", "email", "age")
            .toList();
    }
    
    /**
     * Example: Select specific fields to DTO
     * Equivalent to: users.Select(u => new UserProjection { Username = u.Username, Email = u.Email }).ToList()
     */
    public List<UserProjection> getUserProjections() {
        log.info("Getting user projections using LINQ Select");
        
        return userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select(UserProjection.class)
            .fields("id", "username", "email", "fullName", "age", "status")
            .toList();
    }
    
    /**
     * Example: Select single field
     * Equivalent to: users.Select(u => u.Username).ToList()
     */
    public List<String> getAllUsernames() {
        log.info("Getting all usernames using LINQ Select");
        
        return userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", String.class)
            .toList();
    }
    
    /**
     * Example: Select with pagination
     * Equivalent to: users.Select(u => new { u.Username, u.Email }).Skip(page * size).Take(size)
     */
    public Page<Map<String, Object>> getUserBasicInfoWithPagination(int page, int size) {
        log.info("Getting user basic info with pagination using LINQ Select");
        
        PageRequest pageRequest = PageRequest.of(page, size);
        
        return userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", "email", "fullName")
            .toPage(pageRequest);
    }
    
    /**
     * Example: Complex query with multiple conditions and sorting
     * Equivalent to: users.Where(u => u.Age > 18 && (u.Status == ACTIVE || u.Status == INACTIVE))
     *                     .OrderBy(u => u.Status).ThenByDescending(u => u.Age).ToList()
     */
    public List<User> findAdultUsersWithComplexSorting() {
        log.info("Finding adult users with complex sorting");
        
        return userRepository.linq()
            .where("age", LinqQueryBuilder.WhereOperator.GREATER_THAN, 18)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orWhere("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .orWhere("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.INACTIVE)
            .orderBy("status")
            .thenByDescending("age")
            .toList();
    }
}