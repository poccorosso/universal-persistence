package com.example.persistence;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.entity.User;
import com.example.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
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
                .uid("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .deleted(false)
                .build(),
            User.builder()
                .uid("jane_smith")
                .email("jane@example.com")
                .fullName("Jane Smith")
                .deleted(false)
                .build(),
            User.builder()
                .uid("bob_wilson")
                .email("bob@example.com")
                .fullName("Bob Wilson")
                .deleted(false)
                .build()
        );
        
        userRepository.batchInsert(users);
    }

    @Test
    void testCount() {
        // Test: Count active users
        long count = userRepository.linq()
            .where("deleted", FilterCriteria.FilterOperator.EQUALS, false)
            .count();
        
        assertThat(count).isEqualTo(2);
    }
}
