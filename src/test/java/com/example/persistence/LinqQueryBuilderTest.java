package com.example.persistence;

import com.example.persistence.entity.User;
import com.example.persistence.repository.UserRepository;
import com.example.persistence.util.LinqQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.persistence.dto.FilterCriteria;

/**
 * Test class demonstrating LINQ-style query functionality
 */
@DataJpaTest
@ActiveProfiles("h2")
class LinqQueryBuilderTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Create test data
        List<User> users = Arrays.asList(
                User.builder()
                        .uid("john_doe")
                        .email("john@example.com")
                        .fullName("John Doe")
                        .deleted(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .createdBy("system")
                        .updatedBy("system")
                        .build(),
                User.builder()
                        .uid("jane_smith")
                        .email("jane@example.com")
                        .fullName("Jane Smith")
                        .deleted(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .createdBy("system")
                        .updatedBy("system")
                        .build(),
                User.builder()
                        .uid("bob_wilson")
                        .email("bob@company.com")
                        .fullName("Bob Wilson")
                        .deleted(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .createdBy("system")
                        .updatedBy("system")
                        .build(),
                User.builder()
                        .uid("alice_johnson")
                        .email("alice@example.com")
                        .fullName("Alice Johnson")
                        .deleted(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .createdBy("system")
                        .updatedBy("system")
                        .build(),
                User.builder()
                        .uid("charlie_brown")
                        .email("charlie@example.com")
                        .fullName("Charlie Brown")
                        .deleted(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .createdBy("system")
                        .updatedBy("system")
                        .build()
        );

        userRepository.batchInsert(users);
    }

    @Test
    void testLinqWhereClause() {
        // Test: Find non-deleted users
        List<User> activeUsers = userRepository.linq()
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .toList();

        assertThat(activeUsers).hasSize(5);
    }

    @Test
    void testLinqMultipleConditions() {
        // Test: Find users with specific email domain
        List<User> result = userRepository.linq()
                .where("email", FilterCriteria.FilterOperator.LIKE, "%@example.com")
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .toList();

        assertThat(result).hasSize(4);
    }

    @Test
    void testLinqOrConditions() {
        // Test: Find users with specific uids
        List<User> result = userRepository.linq()
                .where("uid", FilterCriteria.FilterOperator.EQUALS, "john_doe")
                .orWhere("uid", FilterCriteria.FilterOperator.EQUALS, "jane_smith")
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .toList();

        assertThat(result).hasSize(2);
    }

    @Test
    void testLinqOrderBy() {
        // Test: Order by fullName ascending
        List<User> result = userRepository.linq()
                .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
                .orderBy("fullName")
                .toList();

        assertThat(result).hasSize(5);
        assertThat(result.get(0).getFullName()).isEqualTo("Alice Johnson");
    }
}
