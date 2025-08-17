package com.example.persistence;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import com.example.persistence.dto.PageResponse;
import com.example.persistence.entity.User;
import com.example.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User Repository test class demonstrating various query functionalities
 */
@DataJpaTest
@ActiveProfiles("h2")
class UserRepositoryTest {

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
    void testBasicCrud() {
        // Test basic CRUD operations
        User user = User.builder()
                .username("test_user")
                .email("test@example.com")
                .fullName("Test User")
                .age(28)
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();

        // Create
        User saved = userRepository.createOrUpdate(user);
        assertThat(saved.getId()).isNotNull();

        // Read
        User found = userRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("test_user");

        // Update
        found.setAge(29);
        User updated = userRepository.createOrUpdate(found);
        assertThat(updated.getAge()).isEqualTo(29);

        // Delete
        userRepository.delete(updated);
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void testSoftDelete() {
        // Test soft delete functionality
        User user = User.builder()
                .username("soft_delete_user")
                .email("softdelete@example.com")
                .fullName("Soft Delete User")
                .age(28)
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();

        User saved = userRepository.save(user);
        Long userId = saved.getId();

        // Soft delete
        userRepository.softDeleteById(userId);

        // Verify user is soft deleted
        User softDeleted = userRepository.findById(userId).orElse(null);
        assertThat(softDeleted).isNotNull();
        assertThat(softDeleted.getDeleted()).isTrue();

        // Verify active entities query excludes soft deleted
        List<User> activeUsers = userRepository.findActiveEntities();
        assertThat(activeUsers).noneMatch(u -> u.getId().equals(userId));
    }

    @Test
    void testFilterQuery() {
        // Test filter query
        List<FilterCriteria> filters = Arrays.asList(
                FilterCriteria.builder()
                        .field("status")
                        .operator(FilterCriteria.FilterOperator.EQUALS)
                        .value(User.Status.ACTIVE)
                        .build(),
                FilterCriteria.builder()
                        .field("age")
                        .operator(FilterCriteria.FilterOperator.GREATER_THAN)
                        .value(26)
                        .logicalOperator(FilterCriteria.LogicalOperator.AND)
                        .build()
        );

        List<User> result = userRepository.findByFilters(filters);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("jane_smith");
    }

    @Test
    void testPageQuery() {
        // Test pagination query
        PageRequest pageRequest = PageRequest.builder()
                .page(0)
                .size(2)
                .sorts(Arrays.asList(
                        PageRequest.SortCriteria.builder()
                                .field("age")
                                .direction(PageRequest.SortCriteria.SortDirection.DESC)
                                .build()
                ))
                .build();

        List<FilterCriteria> filters = Arrays.asList(
                FilterCriteria.builder()
                        .field("deleted")
                        .operator(FilterCriteria.FilterOperator.EQUALS)
                        .value(false)
                        .build()
        );

        PageResponse<User> result = userRepository.findByFiltersWithPage(filters, pageRequest);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().get(0).getAge()).isGreaterThan(
                result.getContent().get(1).getAge()
        );
    }

    @Test
    void testOptionalPagination() {
        // Test optional pagination
        List<FilterCriteria> filters = Arrays.asList(
                FilterCriteria.builder()
                        .field("status")
                        .operator(FilterCriteria.FilterOperator.EQUALS)
                        .value(User.Status.ACTIVE)
                        .build()
        );

        // Test without pagination
        Object resultWithoutPagination = userRepository.findByFiltersWithOptionalPage(filters, null, false);
        assertThat(resultWithoutPagination).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<User> userList = (List<User>) resultWithoutPagination;
        assertThat(userList).hasSize(2);

        // Test with pagination
        PageRequest pageRequest = PageRequest.builder().page(0).size(1).build();
        Object resultWithPagination = userRepository.findByFiltersWithOptionalPage(filters, pageRequest, true);
        assertThat(resultWithPagination).isInstanceOf(PageResponse.class);
        @SuppressWarnings("unchecked")
        PageResponse<User> pageResponse = (PageResponse<User>) resultWithPagination;
        assertThat(pageResponse.getContent()).hasSize(1);
        assertThat(pageResponse.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testNativeQuery() {
        // Test native SQL query
        String sql = "SELECT username, email, age FROM users WHERE age > :minAge AND deleted = false ORDER BY age";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("minAge", 25);

        List<Map<String, Object>> result = userRepository.executeNativeQuery(sql, parameters);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("USERNAME")).isEqualTo("jane_smith");
        assertThat(result.get(1).get("USERNAME")).isEqualTo("bob_wilson");
    }

    @Test
    void testLikeQuery() {
        // Test LIKE query
        List<FilterCriteria> filters = Arrays.asList(
                FilterCriteria.builder()
                        .field("fullName")
                        .operator(FilterCriteria.FilterOperator.CONTAINS)
                        .value("John")
                        .build()
        );

        List<User> result = userRepository.findByFilters(filters);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).contains("John");
    }

    @Test
    void testInQuery() {
        // Test IN query
        List<FilterCriteria> filters = Collections.singletonList(
                FilterCriteria.builder()
                        .field("username")
                        .operator(FilterCriteria.FilterOperator.IN)
                        .value(Arrays.asList("john_doe", "jane_smith"))
                        .build()
        );

        List<User> result = userRepository.findByFilters(filters);
        assertThat(result).hasSize(2);
    }

    @Test
    void testBetweenQuery() {
        // Test BETWEEN query
        List<FilterCriteria> filters = Arrays.asList(
                FilterCriteria.builder()
                        .field("age")
                        .operator(FilterCriteria.FilterOperator.BETWEEN)
                        .value(Arrays.asList(25, 30))
                        .build()
        );

        List<User> result = userRepository.findByFilters(filters);
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> user.getAge() > 20 && user.getAge() <= 30);
    }
}