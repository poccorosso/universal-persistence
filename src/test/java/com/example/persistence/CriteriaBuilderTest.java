package com.example.persistence;

import com.example.persistence.entity.User;
import com.example.persistence.repository.UserRepository;
import com.example.persistence.util.LinqQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for CriteriaBuilder-based LINQ functionality
 */
@DataJpaTest
@ActiveProfiles("h2")
class CriteriaBuilderTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        // Create test data
        List<User> users = Arrays.asList(
            User.builder()
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .age(25)
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build(),
            User.builder()
                .username("jane_smith")
                .email("jane@example.com")
                .fullName("Jane Smith")
                .age(30)
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build(),
            User.builder()
                .username("bob_wilson")
                .email("bob@example.com")
                .fullName("Bob Wilson")
                .age(35)
                .status(User.Status.INACTIVE)
                .deleted(false)
                .build()
        );
        
        userRepository.batchInsert(users);
    }
    
    @Test
    void testBasicWhereClause() {
        // Test: Find active users
        List<User> activeUsers = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).allMatch(user -> user.getStatus() == User.Status.ACTIVE);
    }
    
    @Test
    void testOrderBy() {
        // Test: Order by age ascending
        List<User> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .toList();
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAge()).isEqualTo(25);
        assertThat(result.get(2).getAge()).isEqualTo(35);
    }
    
    @Test
    void testCount() {
        // Test: Count active users
        long count = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .count();
        
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void testFirst() {
        // Test: Get first user by age
        User youngest = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .first();
        
        assertThat(youngest.getAge()).isEqualTo(25);
        assertThat(youngest.getUsername()).isEqualTo("john_doe");
    }
    
    @Test
    void testAny() {
        // Test: Check if any active users exist
        boolean hasActiveUsers = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .any();
        
        assertThat(hasActiveUsers).isTrue();
    }
    
    @Test
    void testComplexWhereConditions() {
        // Test complex where with lambda syntax
        List<User> complexResult = userRepository.linq()
            .where(w -> w.and(
                w.field("status").eq(User.Status.ACTIVE),
                w.field("age").gt(25)
            ))
            .toList();

        assertThat(complexResult).hasSize(1); // Only jane_smith (age 30)
        assertThat(complexResult.get(0).getUsername()).isEqualTo("jane_smith");
    }

    @Test
    void testProjectionToMap() {
        // Test: Select specific fields as Map
        List<Map<String, Object>> results = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .selectAsMap()
            .field("username")
            .field("age")
            .toList();

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).containsKeys("username", "age");
    }

    @Test
    void testSingleFieldProjection() {
        // Test: Select single field
        List<String> usernames = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .select("username", String.class)
            .toList();

        assertThat(usernames).hasSize(2);
        assertThat(usernames).contains("john_doe", "jane_smith");
    }

    @Test
    void testGroupByCount() {
        // Test: Group by status and count
        Map<Object, Long> statusCounts = userRepository.linq()
            .groupBy("status")
            .count();

        assertThat(statusCounts).hasSize(2);
        assertThat(statusCounts.get(User.Status.ACTIVE)).isEqualTo(2L);
        assertThat(statusCounts.get(User.Status.INACTIVE)).isEqualTo(1L);
    }
}
