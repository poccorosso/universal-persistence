package com.example.persistence;

import com.example.persistence.entity.User;
import com.example.persistence.repository.UserRepository;
import com.example.persistence.util.LinqQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.persistence.dto.FilterCriteria;

/**
 * Test class demonstrating LINQ-style query functionality with lambda expressions
 */
@DataJpaTest
@ActiveProfiles("h2")
class LinqLambdaTest {

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
                        .build()
        );

        userRepository.batchInsert(users);
    }

    @Test
    void testLinqWhereWithLambda() {
        // Test: Find user by email using lambda expression
        // Equivalent to: users.Where(u => u.Email == "john@example.com")
        User user = userRepository.linq()
                .where(User::getEmail, "john@example.com")
                .firstOrDefault();

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getUid()).isEqualTo("john_doe");
    }

    @Test
    void testLinqWhereWithLambdaAndOperator() {
        // Test: Find users with specific email domain using lambda expression
        // Equivalent to: users.Where(u => u.Email.EndsWith("@example.com"))
        List<User> result = userRepository.linq()
                .where(User::getEmail, FilterCriteria.FilterOperator.ENDS_WITH, "@example.com")
                .toList();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getEmail)
                .containsExactlyInAnyOrder("john@example.com", "jane@example.com");
    }

    @Test
    void testLinqOrderByWithLambda() {
        // Test: Order by fullName using lambda expression
        // Equivalent to: users.OrderBy(u => u.FullName)
        List<User> result = userRepository.linq()
                .where(User::getDeleted, false)
                .orderBy(User::getFullName)
                .toList();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getFullName()).isEqualTo("Bob Wilson");
        assertThat(result.get(1).getFullName()).isEqualTo("Jane Smith");
        assertThat(result.get(2).getFullName()).isEqualTo("John Doe");
    }

    @Test
    void testLinqOrderByDescendingWithLambda() {
        // Test: Order by fullName descending using lambda expression
        // Equivalent to: users.OrderByDescending(u => u.FullName)
        List<User> result = userRepository.linq()
                .where(User::getDeleted, false)
                .orderByDescending(User::getFullName)
                .toList();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getFullName()).isEqualTo("John Doe");
        assertThat(result.get(1).getFullName()).isEqualTo("Jane Smith");
        assertThat(result.get(2).getFullName()).isEqualTo("Bob Wilson");
    }

    @Test
    void testLinqSelectWithLambda() {
        // Test: Select specific fields using lambda expressions
        // Equivalent to: users.Select(u => new { u.uid, u.Email })
        List<Object[]> result = userRepository.linq()
                .where(User::getDeleted, false)
                .select(User::getUid, User::getEmail)
                .toObjectList();

        assertThat(result).hasSize(3);
        
        // Verify the selected fields
        assertThat(result).extracting(row -> (String) row[0]) // uid
                .containsExactlyInAnyOrder("john_doe", "jane_smith", "bob_wilson");
        
        assertThat(result).extracting(row -> (String) row[1]) // email
                .containsExactlyInAnyOrder("john@example.com", "jane@example.com", "bob@company.com");
    }

    @Test
    void testLinqChainedOperationsWithLambda() {
        // Test: Chained operations with lambda expressions
        // Equivalent to: users.Where(u => u.Email.EndsWith("@example.com")).OrderBy(u => u.FullName)
        List<User> result = userRepository.linq()
                .where(User::getEmail, FilterCriteria.FilterOperator.ENDS_WITH, "@example.com")
                .where(User::getDeleted, false)
                .orderBy(User::getFullName)
                .toList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("Jane Smith");
        assertThat(result.get(1).getFullName()).isEqualTo("John Doe");
    }
}
