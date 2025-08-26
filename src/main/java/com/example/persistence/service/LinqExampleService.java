package com.example.persistence.service;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.entity.User;
import com.example.persistence.repository.DataSourceRepository;
import com.example.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service demonstrating LINQ-style query usage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinqExampleService {

    private final UserRepository userRepository;
    private final DataSourceRepository dataSourceRepository;


    /**
     * Example: Get first user by uid
     * Equivalent to: users.Where(u => u.uid == uid).FirstOrDefault()
     */
    public User findFirstUserByUid(String uid) {
        log.info("Finding first user by uid: {}", uid);

        return userRepository.linq()
                .where("uid", FilterCriteria.FilterOperator.EQUALS, uid)
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .firstOrDefault();
    }


    /**
     * Example: Check if any users exist with specific email domain
     * Equivalent to: users.Any(u => u.Email.EndsWith("@example.com"))
     */
    public boolean anyUsersWithEmailDomain(String domain) {
        log.info("Checking if any users exist with email domain: {}", domain);

        return userRepository.linq()
                .where("email", FilterCriteria.FilterOperator.ENDS_WITH, domain)
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .any();
    }


    /**
     * Example: Get users with pagination
     * Equivalent to: users.Where(u => u.Status == ACTIVE).OrderByDescending(u => u.CreatedAt).Skip(page * size).Take(size)
     */
    public Page<User> getActiveUsersWithPagination(int page, int size) {
        log.info("Getting active users with pagination: page={}, size={}", page, size);

        PageRequest pageRequest = PageRequest.of(page, size);

        return userRepository.linq()
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .orderByDescending("createdAt")
                .toPage(pageRequest);
    }


    /**
     * Example: Find users with names starting with specific letter
     * Equivalent to: users.Where(u => u.FullName.StartsWith(letter)).OrderBy(u => u.FullName).ToList()
     */
    public List<User> findUsersByNameStartingWith(String letter) {
        log.info("Finding users with names starting with: {}", letter);

        return userRepository.linq()
                .where("fullName", FilterCriteria.FilterOperator.STARTS_WITH, letter)
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .orderBy("fullName")
                .toList();
    }
}