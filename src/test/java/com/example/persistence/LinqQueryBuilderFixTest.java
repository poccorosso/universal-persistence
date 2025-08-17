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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class to verify LinqQueryBuilder bug fixes
 */
@DataJpaTest
@ActiveProfiles("h2")
class LinqQueryBuilderFixTest {
    
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
                .build()
        );
        
        userRepository.batchInsert(users);
    }
    
    @Test
    void testValidationErrors() {
        // Test null field name validation
        assertThatThrownBy(() -> 
            userRepository.linq().where(null, LinqQueryBuilder.WhereOperator.EQUALS, "test")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Field name cannot be null or empty");
        
        // Test empty field name validation
        assertThatThrownBy(() -> 
            userRepository.linq().where("", LinqQueryBuilder.WhereOperator.EQUALS, "test")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Field name cannot be null or empty");
        
        // Test null operator validation
        assertThatThrownBy(() -> 
            userRepository.linq().where("username", null, "test")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Operator cannot be null");
        
        // Test negative take validation
        assertThatThrownBy(() -> 
            userRepository.linq().take(-1)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Take count cannot be negative");
        
        // Test negative skip validation
        assertThatThrownBy(() -> 
            userRepository.linq().skip(-1)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Skip count cannot be negative");
    }
    
    @Test
    void testOperatorValidation() {
        // Test LIKE operator with non-string field should work (it will convert to string)
        List<User> result = userRepository.linq()
            .where("age", LinqQueryBuilder.WhereOperator.LIKE, "25")
            .toList();
        // This might not return results but shouldn't throw an exception
        
        // Test IN operator with empty collection
        List<User> emptyInResult = userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.IN, Arrays.asList())
            .toList();
        assertThat(emptyInResult).isEmpty();
        
        // Test NOT_IN operator with empty collection
        List<User> notInResult = userRepository.linq()
            .where("username", LinqQueryBuilder.WhereOperator.NOT_IN, Arrays.asList())
            .toList();
        assertThat(notInResult).hasSize(3); // Should return all users
        
        // Test BETWEEN with invalid value
        assertThatThrownBy(() -> 
            userRepository.linq()
                .where("age", LinqQueryBuilder.WhereOperator.BETWEEN, "invalid")
                .toList()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("BETWEEN operator requires a List value");
        
        // Test BETWEEN with wrong number of values
        assertThatThrownBy(() -> 
            userRepository.linq()
                .where("age", LinqQueryBuilder.WhereOperator.BETWEEN, Arrays.asList(25))
                .toList()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("BETWEEN operator requires a list with exactly 2 values");
    }
    
    @Test
    void testAnyAndAllMethods() {
        // Test any() method doesn't modify the original builder
        LinqQueryBuilder<User> builder = userRepository.linq()
            .where("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE);
        
        boolean hasActiveUsers = builder.any();
        assertThat(hasActiveUsers).isTrue();
        
        // Original builder should still work
        List<User> activeUsers = builder.toList();
        assertThat(activeUsers).hasSize(2);
        
        // Test any with condition
        boolean hasOldUsers = userRepository.linq()
            .any("age", LinqQueryBuilder.WhereOperator.GREATER_THAN, 30);
        assertThat(hasOldUsers).isTrue();
        
        // Test all method
        boolean allHaveEmail = userRepository.linq()
            .all("email", LinqQueryBuilder.WhereOperator.IS_NOT_NULL, null);
        assertThat(allHaveEmail).isTrue();
        
        boolean allAreActive = userRepository.linq()
            .all("status", LinqQueryBuilder.WhereOperator.EQUALS, User.Status.ACTIVE);
        assertThat(allAreActive).isFalse(); // Bob is INACTIVE
    }
    
    @Test
    void testOrderByWithDifferentTypes() {
        // Test ordering by string field
        List<User> byUsername = userRepository.linq()
            .orderBy("username")
            .toList();
        assertThat(byUsername.get(0).getUsername()).isEqualTo("bob_wilson");
        
        // Test ordering by number field
        List<User> byAge = userRepository.linq()
            .orderBy("age", LinqQueryBuilder.SortDirection.DESC)
            .toList();
        assertThat(byAge.get(0).getAge()).isEqualTo(35);
        
        // Test ordering by enum field
        List<User> byStatus = userRepository.linq()
            .orderBy("status")
            .thenBy("age")
            .toList();
        assertThat(byStatus).hasSize(3);
    }
    
    @Test
    void testSelectProjections() {
        // Test select as Map
        List<Map<String, Object>> basicInfo = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", "email")
            .toList();
        
        assertThat(basicInfo).hasSize(3);
        assertThat(basicInfo.get(0)).containsKeys("username", "email");
        assertThat(basicInfo.get(0)).doesNotContainKey("age");
        
        // Test select single field
        List<String> usernames = userRepository.linq()
            .where("deleted", LinqQueryBuilder.WhereOperator.EQUALS, false)
            .select("username", String.class)
            .toList();
        
        assertThat(usernames).hasSize(3);
        assertThat(usernames).contains("john_doe", "jane_smith", "bob_wilson");
        
        // Test select with null field name should throw exception
        assertThatThrownBy(() -> 
            userRepository.linq()
                .select(String.class)
                .field(null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Field name cannot be null or empty");
    }
    
    @Test
    void testComplexWhereConditions() {
        // Test complex where with lambda syntax
        List<User> complexResult = userRepository.linq()
            .where(w -> w.and(
                w.field("status").eq(User.Status.ACTIVE),
                w.or(
                    w.field("age").gt(25),
                    w.field("username").contains("john")
                )
            ))
            .toList();
        
        assertThat(complexResult).hasSize(2); // john_doe and jane_smith
        
        // Test null where function should throw exception
        assertThatThrownBy(() -> 
            userRepository.linq().where(null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Where function cannot be null");
    }
    
    @Test
    void testPaginationValidation() {
        // Test null pageable should throw exception
        assertThatThrownBy(() -> 
            userRepository.linq().toPage(null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Pageable cannot be null");
        
        // Test projection with null pageable should throw exception
        assertThatThrownBy(() -> 
            userRepository.linq()
                .select("username", "email")
                .toPage(null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Pageable cannot be null");
    }
}