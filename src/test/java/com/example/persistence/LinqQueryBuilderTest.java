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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
                .email("bob@company.com")
                .fullName("Bob Wilson")
                .age(35)
                .status(User.Status.INACTIVE)
                .deleted(false)
                .build(),
            User.builder()
                .username("alice_johnson")
                .email("alice@example.com")
                .fullName("Alice Johnson")
                .age(28)
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build(),
            User.builder()
                .username("charlie_brown")
                .email("charlie@example.com")
                .fullName("Charlie Brown")
                .age(22)
                .status(User.Status.SUSPENDED)
                .deleted(false)
                .build()
        );
        
        userRepository.batchInsert(users);
    }
    
    @Test
    void testLinqWhereClause() {
        // Test: Find active users
        // Equivalent to: users.Where(u => u.Status == ACTIVE).ToList()
        List<User> activeUsers = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(activeUsers).hasSize(3);
        assertThat(activeUsers).allMatch(user -> user.getStatus() == User.Status.ACTIVE);
    }
    
    @Test
    void testLinqMultipleConditions() {
        // Test: Find active users older than 25
        // Equivalent to: users.Where(u => u.Status == ACTIVE && u.Age > 25).ToList()
        List<User> result = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .where("age", LinqQueryBuilder.WhereOperator.GREATER_THAN, 25)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> 
            user.getStatus() == User.Status.ACTIVE && user.getAge() > 25);
    }
    
    @Test
    void testLinqOrConditions() {
        // Test: Find users who are either very young or very old
        // Equivalent to: users.Where(u => u.Age < 25 || u.Age > 30).ToList()
        List<User> result = userRepository.linq()
            .where("age", LinqQueryBuilder.WhereOperator.LESS_THAN, 25)
            .orWhere("age", LinqQueryBuilder.WhereOperator.GREATER_THAN, 30)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> user.getAge() < 25 || user.getAge() > 30);
    }
    
    @Test
    void testLinqComplexConditions() {
        // Test: Complex conditions using lambda-like syntax
        List<User> result = userRepository.linq()
            .where(w -> w.and(
                w.field("status").eq(User.Status.ACTIVE),
                w.or(
                    w.field("age").gt(25),
                    w.field("username").contains("alice")
                )
            ))
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(result).hasSize(3);
    }
    
    @Test
    void testLinqOrderBy() {
        // Test: Order by age ascending
        // Equivalent to: users.OrderBy(u => u.Age).ToList()
        List<User> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .toList();
        
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getAge()).isEqualTo(22);
        assertThat(result.get(4).getAge()).isEqualTo(35);
    }
    
    @Test
    void testLinqOrderByDescending() {
        // Test: Order by age descending
        // Equivalent to: users.OrderByDescending(u => u.Age).ToList()
        List<User> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderByDescending("age")
            .toList();
        
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getAge()).isEqualTo(35);
        assertThat(result.get(4).getAge()).isEqualTo(22);
    }
    
    @Test
    void testLinqThenBy() {
        // Test: Order by status, then by age
        // Equivalent to: users.OrderBy(u => u.Status).ThenBy(u => u.Age).ToList()
        List<User> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("status")
            .thenBy("age")
            .toList();
        
        assertThat(result).hasSize(5);
        // First should be ACTIVE users ordered by age
        assertThat(result.get(0).getStatus()).isEqualTo(User.Status.ACTIVE);
        assertThat(result.get(0).getAge()).isEqualTo(25); // John, youngest ACTIVE
    }
    
    @Test
    void testLinqTake() {
        // Test: Take first 3 users
        // Equivalent to: users.Take(3).ToList()
        List<User> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .take(3)
            .toList();
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAge()).isEqualTo(22);
        assertThat(result.get(2).getAge()).isEqualTo(28);
    }
    
    @Test
    void testLinqSkip() {
        // Test: Skip first 2 users
        // Equivalent to: users.Skip(2).ToList()
        List<User> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .skip(2)
            .toList();
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAge()).isEqualTo(28);
    }
    
    @Test
    void testLinqSkipAndTake() {
        // Test: Skip 1 and take 2 (pagination)
        // Equivalent to: users.Skip(1).Take(2).ToList()
        List<User> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .skip(1)
            .take(2)
            .toList();
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAge()).isEqualTo(25);
        assertThat(result.get(1).getAge()).isEqualTo(28);
    }
    
    @Test
    void testLinqFirst() {
        // Test: Get first user
        // Equivalent to: users.First()
        User result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .first();
        
        assertThat(result).isNotNull();
        assertThat(result.getAge()).isEqualTo(22);
        assertThat(result.getUsername()).isEqualTo("charlie_brown");
    }
    
    @Test
    void testLinqFirstOrDefault() {
        // Test: Get first user or null
        // Equivalent to: users.FirstOrDefault()
        User result = userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.EQUALS, "nonexistent")
            .firstOrDefault();
        
        assertThat(result).isNull();
        
        // Test with existing user
        User existingUser = userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.EQUALS, "john_doe")
            .firstOrDefault();
        
        assertThat(existingUser).isNotNull();
        assertThat(existingUser.getUsername()).isEqualTo("john_doe");
    }
    
    @Test
    void testLinqSingle() {
        // Test: Get single user by unique field
        // Equivalent to: users.Single(u => u.Username == "john_doe")
        User result = userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.EQUALS, "john_doe")
            .single();
        
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john_doe");
    }
    
    @Test
    void testLinqCount() {
        // Test: Count users
        // Equivalent to: users.Count()
        long totalCount = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .count();
        
        assertThat(totalCount).isEqualTo(5);
        
        // Count with condition
        long activeCount = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .count();
        
        assertThat(activeCount).isEqualTo(3);
    }
    
    @Test
    void testLinqAny() {
        // Test: Check if any users exist
        // Equivalent to: users.Any()
        boolean hasUsers = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .any();
        
        assertThat(hasUsers).isTrue();
        
        // Test with condition
        boolean hasSuspendedUsers = userRepository.linq()
            .any("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.SUSPENDED);
        
        assertThat(hasSuspendedUsers).isTrue();
        
        // Test with non-matching condition
        boolean hasOldUsers = userRepository.linq()
            .any("age", LinqQueryBuilder.WhereOperator.GREATER_THAN, 100);
        
        assertThat(hasOldUsers).isFalse();
    }
    
    @Test
    void testLinqStringOperators() {
        // Test: String contains
        // Equivalent to: users.Where(u => u.FullName.Contains("John")).ToList()
        List<User> containsJohn = userRepository.linq()
            .where("fullName", LinqQueryBuilder.WhereOperator.CONTAINS, "John")
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(containsJohn).hasSize(2); // John Doe and Alice Johnson
        
        // Test: String starts with
        List<User> startsWithJ = userRepository.linq()
            .where("fullName", LinqQueryBuilder.WhereOperator.STARTS_WITH, "J")
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(startsWithJ).hasSize(2); // John Doe and Jane Smith
        
        // Test: String ends with
        List<User> endsWithCom = userRepository.linq()
            .where("email", LinqQueryBuilder.WhereOperator.ENDS_WITH, "example.com")
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(endsWithCom).hasSize(4);
    }
    
    @Test
    void testLinqInOperator() {
        // Test: IN operator
        // Equivalent to: users.Where(u => new[] { "john_doe", "jane_smith" }.Contains(u.Username)).ToList()
        List<String> usernames = Arrays.asList("john_doe", "jane_smith");
        List<User> result = userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.IN, usernames)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> usernames.contains(user.getUsername()));
    }
    
    @Test
    void testLinqBetweenOperator() {
        // Test: BETWEEN operator
        // Equivalent to: users.Where(u => u.Age >= 25 && u.Age <= 30).ToList()
        List<Integer> ageRange = Arrays.asList(25, 30);
        List<User> result = userRepository.linq()
            .where("age", LinqQueryBuilder.WhereOperator.BETWEEN, ageRange)
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .toList();
        
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(user -> user.getAge() >= 25 && user.getAge() <= 30);
    }
    
    @Test
    void testLinqPagination() {
        // Test: Pagination using toPage
        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<User> page = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .orderBy("age")
            .toPage(pageRequest);
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getContent().get(0).getAge()).isEqualTo(22);
        assertThat(page.getContent().get(1).getAge()).isEqualTo(25);
    }
    
    @Test
    void testLinqGroupBy() {
        // Test: Group by status
        Map<Object, Long> groupCounts = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .groupBy("status")
            .count();
        
        assertThat(groupCounts).hasSize(3);
        assertThat(groupCounts.get(User.Status.ACTIVE)).isEqualTo(3);
        assertThat(groupCounts.get(User.Status.INACTIVE)).isEqualTo(1);
        assertThat(groupCounts.get(User.Status.SUSPENDED)).isEqualTo(1);
    }
    
    @Test
    void testLinqSelectAsMap() {
        // Test: Select specific fields as Map
        // Equivalent to: users.Select(u => new { u.Username, u.Email }).ToList()
        List<Map<String, Object>> result = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", "email")
            .toList();
        
        assertThat(result).hasSize(5);
        assertThat(result.get(0)).containsKeys("username", "email");
        assertThat(result.get(0)).doesNotContainKey("fullName");
    }
    
    @Test
    void testLinqSelectSingleField() {
        // Test: Select single field
        // Equivalent to: users.Select(u => u.Username).ToList()
        List<String> usernames = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", String.class)
            .toList();
        
        assertThat(usernames).hasSize(5);
        assertThat(usernames).contains("john_doe", "jane_smith", "bob_wilson", "alice_johnson", "charlie_brown");
    }
    
    @Test
    void testLinqSelectWithPagination() {
        // Test: Select with pagination
        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<Map<String, Object>> page = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", "age")
            .toPage(pageRequest);
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent().get(0)).containsKeys("username", "age");
    }
    
    @Test
    void testLinqSelectFirstOrDefault() {
        // Test: Select first result
        Map<String, Object> result = userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.EQUALS, "john_doe")
            .select("username", "email", "age")
            .firstOrDefault();
        
        assertThat(result).isNotNull();
        assertThat(result.get("username")).isEqualTo("john_doe");
        assertThat(result).containsKeys("username", "email", "age");
    }
}