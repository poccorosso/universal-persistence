package com.example.persistence.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.function.Function;

/**
 * LINQ-style query builder for fluent query construction using JPA CriteriaBuilder
 */
@Slf4j
public class LinqQueryBuilder<T> {

    private final Class<T> entityClass;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final List<Predicate> wherePredicates;
    private final List<Predicate> orPredicates;
    private final List<Order> orderList;
    private Integer limitValue;
    private Integer offsetValue;

    public LinqQueryBuilder(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
        this.wherePredicates = new ArrayList<>();
        this.orPredicates = new ArrayList<>();
        this.orderList = new ArrayList<>();
        log.debug("Created LinqQueryBuilder for entity: {}", entityClass.getSimpleName());
    }

    /**
     * Add WHERE condition (equivalent to LINQ Where)
     */
    public LinqQueryBuilder<T> where(String fieldName, Object value) {
        return where(fieldName, WhereOperator.EQUALS, value);
    }

    /**
     * Add WHERE condition with operator
     */
    public LinqQueryBuilder<T> where(String fieldName, WhereOperator operator, Object value) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }

        try {
            CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            Path<?> path = getFieldPath(root, fieldName);
            Predicate predicate = buildPredicate(path, operator, value);
            wherePredicates.add(predicate);
            log.trace("Added WHERE condition: {} {} {}", fieldName, operator, value);
        } catch (Exception e) {
            log.error("Failed to add WHERE condition: {} {} {}", fieldName, operator, value, e);
            throw new RuntimeException("Failed to add WHERE condition: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Add OR WHERE condition
     */
    public LinqQueryBuilder<T> orWhere(String fieldName, WhereOperator operator, Object value) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }

        try {
            CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            Path<?> path = getFieldPath(root, fieldName);
            Predicate predicate = buildPredicate(path, operator, value);
            orPredicates.add(predicate);
            log.trace("Added OR WHERE condition: {} {} {}", fieldName, operator, value);
        } catch (Exception e) {
            log.error("Failed to add OR WHERE condition: {} {} {}", fieldName, operator, value, e);
            throw new RuntimeException("Failed to add OR WHERE condition: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Add complex WHERE condition using lambda-like syntax
     */
    public LinqQueryBuilder<T> where(Function<WhereBuilder<T>, Predicate> whereFunction) {
        if (whereFunction == null) {
            throw new IllegalArgumentException("Where function cannot be null");
        }

        try {
            CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            WhereBuilder<T> builder = new WhereBuilder<>(root, criteriaBuilder);
            Predicate predicate = whereFunction.apply(builder);
            if (predicate != null) {
                wherePredicates.add(predicate);
                log.trace("Added complex WHERE condition");
            }
        } catch (Exception e) {
            log.error("Failed to add complex WHERE condition", e);
            throw new RuntimeException("Failed to add complex WHERE condition: " + e.getMessage(), e);
        }
        return this;
    }
    
    /**
     * Add ORDER BY ascending (equivalent to LINQ OrderBy)
     */
    public LinqQueryBuilder<T> orderBy(String fieldName) {
        return orderBy(fieldName, SortDirection.ASC);
    }

    /**
     * Add ORDER BY with direction
     */
    public LinqQueryBuilder<T> orderBy(String fieldName, SortDirection direction) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (direction == null) {
            direction = SortDirection.ASC;
        }

        try {
            CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            Path<?> path = getFieldPath(root, fieldName);

            Order order = direction == SortDirection.DESC ?
                criteriaBuilder.desc(path) : criteriaBuilder.asc(path);

            orderList.add(order);
            log.trace("Added ORDER BY: {} {}", fieldName, direction);
        } catch (Exception e) {
            log.error("Failed to add ORDER BY: {} {}", fieldName, direction, e);
            throw new RuntimeException("Failed to add ORDER BY: " + e.getMessage(), e);
        }
        return this;
    }
    
    /**
     * Add ORDER BY descending (equivalent to LINQ OrderByDescending)
     */
    public LinqQueryBuilder<T> orderByDescending(String fieldName) {
        return orderBy(fieldName, SortDirection.DESC);
    }
    
    /**
     * Add secondary ORDER BY ascending (equivalent to LINQ ThenBy)
     */
    public LinqQueryBuilder<T> thenBy(String fieldName) {
        return orderBy(fieldName, SortDirection.ASC);
    }
    
    /**
     * Add secondary ORDER BY descending (equivalent to LINQ ThenByDescending)
     */
    public LinqQueryBuilder<T> thenByDescending(String fieldName) {
        return orderBy(fieldName, SortDirection.DESC);
    }
    
    /**
     * Set LIMIT (equivalent to LINQ Take)
     */
    public LinqQueryBuilder<T> take(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Take count cannot be negative");
        }
        this.limitValue = count;
        log.trace("Set TAKE: {}", count);
        return this;
    }
    
    /**
     * Set OFFSET (equivalent to LINQ Skip)
     */
    public LinqQueryBuilder<T> skip(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Skip count cannot be negative");
        }
        this.offsetValue = count;
        log.trace("Set SKIP: {}", count);
        return this;
    }
    
    /**
     * Execute query and return list (equivalent to LINQ ToList)
     */
    public List<T> toList() {
        log.debug("Executing query to list");
        TypedQuery<T> query = buildQuery();
        List<T> results = query.getResultList();
        log.debug("Query returned {} results", results.size());
        return results;
    }

    /**
     * Execute query and return first result (equivalent to LINQ First)
     */
    public T first() {
        log.debug("Executing query to get first result");
        TypedQuery<T> query = buildQuery();
        query.setMaxResults(1);
        List<T> results = query.getResultList();
        if (results.isEmpty()) {
            throw new RuntimeException("No elements found");
        }
        return results.get(0);
    }

    /**
     * Execute query and return first result or null (equivalent to LINQ FirstOrDefault)
     */
    public T firstOrDefault() {
        log.debug("Executing query to get first result or default");
        TypedQuery<T> query = buildQuery();
        query.setMaxResults(1);
        List<T> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Execute query and return single result (equivalent to LINQ Single)
     */
    public T single() {
        log.debug("Executing query to get single result");
        TypedQuery<T> query = buildQuery();
        List<T> results = query.getResultList();
        if (results.isEmpty()) {
            throw new RuntimeException("No elements found");
        }
        if (results.size() > 1) {
            throw new RuntimeException("More than one element found");
        }
        return results.get(0);
    }

    /**
     * Execute query and return single result or null (equivalent to LINQ SingleOrDefault)
     */
    public T singleOrDefault() {
        log.debug("Executing query to get single result or default");
        TypedQuery<T> query = buildQuery();
        List<T> results = query.getResultList();
        if (results.size() > 1) {
            throw new RuntimeException("More than one element found");
        }
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Execute query and return count (equivalent to LINQ Count)
     */
    public long count() {
        log.debug("Executing count query");
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = countQuery.from(entityClass);
        countQuery.select(criteriaBuilder.count(root));

        // Apply where conditions
        Predicate finalPredicate = buildFinalPredicate(root);
        if (finalPredicate != null) {
            countQuery.where(finalPredicate);
        }

        TypedQuery<Long> query = entityManager.createQuery(countQuery);
        long result = query.getSingleResult();
        log.debug("Count query returned: {}", result);
        return result;
    }

    /**
     * Check if any results exist (equivalent to LINQ Any)
     */
    public boolean any() {
        log.debug("Executing any query");
        return count() > 0;
    }

    /**
     * Check if any results exist with condition
     */
    public boolean any(String fieldName, WhereOperator operator, Object value) {
        // Create a new builder to avoid modifying current builder
        LinqQueryBuilder<T> conditionBuilder = new LinqQueryBuilder<>(entityClass, entityManager);

        // Copy current conditions
        conditionBuilder.wherePredicates.addAll(this.wherePredicates);
        conditionBuilder.orPredicates.addAll(this.orPredicates);

        // Add the new condition
        conditionBuilder.where(fieldName, operator, value);

        return conditionBuilder.any();
    }

    /**
     * Check if all results match condition (equivalent to LINQ All)
     */
    public boolean all(String fieldName, WhereOperator operator, Object value) {
        log.debug("Executing all query");
        // Get total count with current conditions
        long totalCount = count();
        if (totalCount == 0) {
            return true; // vacuously true
        }

        // Create a new builder with the additional condition to avoid modifying current builder
        LinqQueryBuilder<T> conditionBuilder = new LinqQueryBuilder<>(entityClass, entityManager);

        // Copy current conditions
        conditionBuilder.wherePredicates.addAll(this.wherePredicates);
        conditionBuilder.orPredicates.addAll(this.orPredicates);

        // Add the new condition
        conditionBuilder.where(fieldName, operator, value);

        // Get count with additional condition
        long conditionCount = conditionBuilder.count();
        return totalCount == conditionCount;
    }
    
    /**
     * Execute query with pagination
     */
    public Page<T> toPage(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }

        log.debug("Executing query with pagination: page={}, size={}",
                 pageable.getPageNumber(), pageable.getPageSize());

        // Get total count first (without pagination)
        long totalCount = count();

        // Build query with pagination
        TypedQuery<T> query = buildQuery();

        // Apply pagination
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<T> content = query.getResultList();

        log.debug("Page query returned {} results out of {} total", content.size(), totalCount);
        return new PageImpl<>(content, pageable, totalCount);
    }
    
    /**
     * Select specific fields (equivalent to LINQ Select)
     */
    public <R> LinqProjectionBuilder<T, R> select(Class<R> resultType) {
        return new LinqProjectionBuilder<>(this, resultType);
    }

    /**
     * Select fields and return as Map
     */
    public LinqProjectionBuilder<T, Map<String, Object>> selectAsMap() {
        return new LinqProjectionBuilder<>(this, (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    /**
     * Select single field
     */
    public <R> LinqProjectionBuilder<T, R> select(String fieldName, Class<R> resultType) {
        return new LinqProjectionBuilder<T, R>(this, resultType).field(fieldName);
    }

    /**
     * Select multiple fields as Map
     */
    public LinqProjectionBuilder<T, Map<String, Object>> select(String... fieldNames) {
        LinqProjectionBuilder<T, Map<String, Object>> builder = selectAsMap();
        for (String fieldName : fieldNames) {
            builder.field(fieldName);
        }
        return builder;
    }

    /**
     * Group by field (equivalent to LINQ GroupBy)
     */
    public LinqGroupBuilder<T> groupBy(String fieldName) {
        return new LinqGroupBuilder<>(this, fieldName);
    }
    
    /**
     * Build the TypedQuery
     */
    TypedQuery<T> buildQuery() {
        CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        query.select(root);

        // Apply where conditions
        Predicate finalPredicate = buildFinalPredicate(root);
        if (finalPredicate != null) {
            query.where(finalPredicate);
        }

        // Apply ordering
        if (!orderList.isEmpty()) {
            query.orderBy(orderList);
        }

        TypedQuery<T> typedQuery = entityManager.createQuery(query);

        if (offsetValue != null) {
            typedQuery.setFirstResult(offsetValue);
        }

        if (limitValue != null) {
            typedQuery.setMaxResults(limitValue);
        }

        return typedQuery;
    }

    /**
     * Build final predicate combining AND and OR conditions
     */
    private Predicate buildFinalPredicate(Root<T> root) {
        Predicate finalPredicate = null;

        if (!wherePredicates.isEmpty()) {
            finalPredicate = criteriaBuilder.and(wherePredicates.toArray(new Predicate[0]));
        }

        if (!orPredicates.isEmpty()) {
            Predicate orPredicate = criteriaBuilder.or(orPredicates.toArray(new Predicate[0]));
            finalPredicate = finalPredicate == null ? orPredicate :
                criteriaBuilder.and(finalPredicate, orPredicate);
        }

        return finalPredicate;
    }

    /**
     * Get entity manager (package-private for use by other builders)
     */
    EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Get criteria builder (package-private for use by other builders)
     */
    CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }
    
    /**
     * Build predicate for field and operator
     */
    @SuppressWarnings("unchecked")
    private Predicate buildPredicate(Path<?> path, WhereOperator operator, Object value) {
        try {
            switch (operator) {
                case EQUALS:
                    return criteriaBuilder.equal(path, value);
                case NOT_EQUALS:
                    return criteriaBuilder.notEqual(path, value);
                case GREATER_THAN:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for GREATER_THAN operator");
                    }
                    return criteriaBuilder.greaterThan((Path<Comparable>) path, (Comparable) value);
                case GREATER_THAN_OR_EQUAL:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for GREATER_THAN_OR_EQUAL operator");
                    }
                    return criteriaBuilder.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
                case LESS_THAN:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for LESS_THAN operator");
                    }
                    return criteriaBuilder.lessThan((Path<Comparable>) path, (Comparable) value);
                case LESS_THAN_OR_EQUAL:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for LESS_THAN_OR_EQUAL operator");
                    }
                    return criteriaBuilder.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
                case LIKE:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for LIKE operator");
                    }
                    return criteriaBuilder.like((Path<String>) path, "%" + value + "%");
                case NOT_LIKE:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for NOT_LIKE operator");
                    }
                    return criteriaBuilder.notLike((Path<String>) path, "%" + value + "%");
                case STARTS_WITH:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for STARTS_WITH operator");
                    }
                    return criteriaBuilder.like((Path<String>) path, value.toString() + "%");
                case ENDS_WITH:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for ENDS_WITH operator");
                    }
                    return criteriaBuilder.like((Path<String>) path, "%" + value.toString());
                case CONTAINS:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for CONTAINS operator");
                    }
                    return criteriaBuilder.like((Path<String>) path, "%" + value.toString() + "%");
                case IN:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for IN operator");
                    }
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException("IN operator requires a Collection value");
                    }
                    Collection<?> collection = (Collection<?>) value;
                    if (collection.isEmpty()) {
                        // Return a condition that will never match
                        return criteriaBuilder.equal(path, new Object());
                    }
                    return path.in(collection);
                case NOT_IN:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for NOT_IN operator");
                    }
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException("NOT_IN operator requires a Collection value");
                    }
                    Collection<?> notInCollection = (Collection<?>) value;
                    if (notInCollection.isEmpty()) {
                        // Return a condition that will always match (except null values)
                        return criteriaBuilder.isNotNull(path);
                    }
                    return criteriaBuilder.not(path.in(notInCollection));
                case IS_NULL:
                    return criteriaBuilder.isNull(path);
                case IS_NOT_NULL:
                    return criteriaBuilder.isNotNull(path);
                case BETWEEN:
                    if (value == null) {
                        throw new IllegalArgumentException("Value cannot be null for BETWEEN operator");
                    }
                    if (!(value instanceof List)) {
                        throw new IllegalArgumentException("BETWEEN operator requires a List value");
                    }
                    List<?> values = (List<?>) value;
                    if (values.size() != 2) {
                        throw new IllegalArgumentException("BETWEEN operator requires a list with exactly 2 values");
                    }
                    if (values.get(0) == null || values.get(1) == null) {
                        throw new IllegalArgumentException("BETWEEN operator values cannot be null");
                    }
                    return criteriaBuilder.between((Path<Comparable>) path,
                        (Comparable) values.get(0), (Comparable) values.get(1));
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid value type for operator " + operator + " on field path " + path, e);
        }
    }
    
    /**
     * Get field path by name
     */
    private Path<?> getFieldPath(Root<T> root, String fieldName) {
        try {
            // Handle nested field paths (e.g., "user.name")
            String[] fieldParts = fieldName.split("\\.");
            Path<?> path = root;

            for (String fieldPart : fieldParts) {
                path = path.get(fieldPart);
            }

            return path;
        } catch (Exception e) {
            log.error("Failed to get field path: {}", fieldName, e);
            throw new RuntimeException("Failed to get field path: " + fieldName, e);
        }
    }
    
    /**
     * Where operators enum
     */
    public enum WhereOperator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        LIKE,
        NOT_LIKE,
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS,
        IN,
        NOT_IN,
        IS_NULL,
        IS_NOT_NULL,
        BETWEEN
    }
    
    /**
     * Sort direction enum
     */
    public enum SortDirection {
        ASC, DESC
    }
}