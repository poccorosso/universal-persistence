package com.example.persistence;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.entity.User;
import com.example.persistence.util.QueryBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for QueryBuilder
 */
@SpringBootTest
@ActiveProfiles("h2")
class QueryBuilderTest {

    @Autowired
    private EntityManager entityManager;

    private QueryBuilder<User> queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new QueryBuilder<>(User.class, entityManager);
    }

    @Test
    void testBuildWhereClauseWithSingleFilter() {
        FilterCriteria filter = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("testuser")
                .build();

        List<FilterCriteria> filters = Arrays.asList(filter);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE username = :username");
    }

    @Test
    void testBuildWhereClauseWithMultipleFilters() {
        FilterCriteria filter1 = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("testuser")
                .build();

        FilterCriteria filter2 = FilterCriteria.builder()
                .field("email")
                .operator(FilterCriteria.FilterOperator.LIKE)
                .value("@example.com")
                .build();

        List<FilterCriteria> filters = Arrays.asList(filter1, filter2);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE username = :username AND email LIKE :email");
    }

    @Test
    void testBuildWhereClauseWithOrOperator() {
        FilterCriteria filter1 = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("user1")
                .logicalOperator(FilterCriteria.LogicalOperator.OR)
                .build();

        FilterCriteria filter2 = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("user2")
                .logicalOperator(FilterCriteria.LogicalOperator.OR)
                .build();

        List<FilterCriteria> filters = Arrays.asList(filter1, filter2);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE (username = :username OR username = :username)");
    }

    @Test
    void testBuildWhereClauseWithMixedOperators() {
        FilterCriteria filter1 = FilterCriteria.builder()
                .field("status")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("ACTIVE")
                .build();

        FilterCriteria filter2 = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("user1")
                .logicalOperator(FilterCriteria.LogicalOperator.OR)
                .build();

        FilterCriteria filter3 = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("user2")
                .logicalOperator(FilterCriteria.LogicalOperator.OR)
                .build();

        List<FilterCriteria> filters = Arrays.asList(filter1, filter2, filter3);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE status = :status AND (username = :username OR username = :username)");
    }

    @Test
    void testBuildQueryWithOrderBy() {
        FilterCriteria filter = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("testuser")
                .build();

        List<FilterCriteria> filters = Arrays.asList(filter);
        String query = queryBuilder.buildQuery(filters, "username", org.springframework.data.domain.Sort.Direction.ASC);

        assertThat(query).isEqualTo("FROM User WHERE username = :username ORDER BY username ASC");
    }

    @Test
    void testBuildCountQuery() {
        FilterCriteria filter = FilterCriteria.builder()
                .field("status")
                .operator(FilterCriteria.FilterOperator.EQUALS)
                .value("ACTIVE")
                .build();

        List<FilterCriteria> filters = Arrays.asList(filter);
        String countQuery = queryBuilder.buildCountQuery(filters);

        assertThat(countQuery).isEqualTo("SELECT COUNT(*) FROM User WHERE status = :status");
    }

    @Test
    void testBuildWhereClauseWithNullFilters() {
        String whereClause = queryBuilder.buildWhereClause(null);
        assertThat(whereClause).isEmpty();

        String whereClauseEmpty = queryBuilder.buildWhereClause(Arrays.asList());
        assertThat(whereClauseEmpty).isEmpty();
    }

    @Test
    void testBuildWhereClauseWithLikeOperators() {
        FilterCriteria startsWith = FilterCriteria.builder()
                .field("username")
                .operator(FilterCriteria.FilterOperator.STARTS_WITH)
                .value("test")
                .build();

        FilterCriteria endsWith = FilterCriteria.builder()
                .field("email")
                .operator(FilterCriteria.FilterOperator.ENDS_WITH)
                .value(".com")
                .build();

        FilterCriteria contains = FilterCriteria.builder()
                .field("fullName")
                .operator(FilterCriteria.FilterOperator.CONTAINS)
                .value("john")
                .build();

        List<FilterCriteria> filters = Arrays.asList(startsWith, endsWith, contains);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE username LIKE :username AND email LIKE :email AND fullName LIKE :fullName");
    }

    @Test
    void testBuildWhereClauseWithInOperator() {
        FilterCriteria inFilter = FilterCriteria.builder()
                .field("status")
                .operator(FilterCriteria.FilterOperator.IN)
                .value(Arrays.asList("ACTIVE", "PENDING"))
                .build();

        List<FilterCriteria> filters = Arrays.asList(inFilter);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE status IN :status");
    }

    @Test
    void testBuildWhereClauseWithBetweenOperator() {
        FilterCriteria betweenFilter = FilterCriteria.builder()
                .field("age")
                .operator(FilterCriteria.FilterOperator.BETWEEN)
                .value(Arrays.asList(18, 65))
                .build();

        List<FilterCriteria> filters = Arrays.asList(betweenFilter);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE age BETWEEN :age_from AND :age_to");
    }

    @Test
    void testBuildWhereClauseWithNullCheck() {
        FilterCriteria isNull = FilterCriteria.builder()
                .field("deletedAt")
                .operator(FilterCriteria.FilterOperator.IS_NULL)
                .build();

        FilterCriteria isNotNull = FilterCriteria.builder()
                .field("updatedAt")
                .operator(FilterCriteria.FilterOperator.IS_NOT_NULL)
                .build();

        List<FilterCriteria> filters = Arrays.asList(isNull, isNotNull);
        String whereClause = queryBuilder.buildWhereClause(filters);

        assertThat(whereClause).isEqualTo(" WHERE deletedAt IS NULL AND updatedAt IS NOT NULL");
    }
}
