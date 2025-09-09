package com.example.persistence.controller;

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
}
