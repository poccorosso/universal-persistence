package com.example.persistence.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for SELECT projections (equivalent to LINQ Select) using JPA CriteriaBuilder
 */
@Slf4j
public class LinqProjectionBuilder<T, R> {

    private final LinqQueryBuilder<T> queryBuilder;
    private final Class<T> entityClass;
    private final Class<R> resultType;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final List<Selection<?>> projections;
    private final List<String> fieldNames;

    public LinqProjectionBuilder(LinqQueryBuilder<T> queryBuilder, Class<R> resultType) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException("QueryBuilder cannot be null");
        }
        if (resultType == null) {
            throw new IllegalArgumentException("Result type cannot be null");
        }
        this.queryBuilder = queryBuilder;
        this.resultType = resultType;
        this.entityManager = queryBuilder.getEntityManager();
        this.criteriaBuilder = queryBuilder.getCriteriaBuilder();
        this.projections = new ArrayList<>();
        this.fieldNames = new ArrayList<>();

        // Get entity class from query builder
        this.entityClass = getEntityClassFromQueryBuilder(queryBuilder);
    }

    @SuppressWarnings("unchecked")
    private Class<T> getEntityClassFromQueryBuilder(LinqQueryBuilder<T> queryBuilder) {
        try {
            // Use reflection to get the entity class from the query builder
            java.lang.reflect.Field field = queryBuilder.getClass().getDeclaredField("entityClass");
            field.setAccessible(true);
            return (Class<T>) field.get(queryBuilder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get entity class from query builder", e);
        }
    }

    /**
     * Add field to projection
     */
    public LinqProjectionBuilder<T, R> field(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        try {
            CriteriaQuery<Object> tempQuery = criteriaBuilder.createQuery(Object.class);
            Root<T> root = tempQuery.from(entityClass);
            Path<?> fieldPath = getFieldPath(root, fieldName);
            projections.add(fieldPath);
            fieldNames.add(fieldName);
            log.trace("Added field to projection: {}", fieldName);
        } catch (Exception e) {
            log.error("Failed to add field to projection: {}", fieldName, e);
            throw new RuntimeException("Failed to add field to projection: " + fieldName, e);
        }
        return this;
    }

    /**
     * Add field with alias
     */
    public LinqProjectionBuilder<T, R> field(String fieldName, String alias) {
        try {
            CriteriaQuery<Object> tempQuery = criteriaBuilder.createQuery(Object.class);
            Root<T> root = tempQuery.from(entityClass);
            Path<?> fieldPath = getFieldPath(root, fieldName);
            projections.add(fieldPath.alias(alias));
            fieldNames.add(alias);
            log.trace("Added field to projection: {} as {}", fieldName, alias);
        } catch (Exception e) {
            log.error("Failed to add field to projection: {} as {}", fieldName, alias, e);
            throw new RuntimeException("Failed to add field to projection: " + fieldName + " as " + alias, e);
        }
        return this;
    }

    /**
     * Add multiple fields to projection
     */
    public LinqProjectionBuilder<T, R> fields(String... fieldNames) {
        for (String fieldName : fieldNames) {
            field(fieldName);
        }
        return this;
    }

    /**
     * Execute projection query and return list
     */
    public List<R> toList() {
        log.debug("Executing projection query to list");

        if (projections.isEmpty()) {
            throw new IllegalStateException("No fields specified for projection");
        }

        // Create projection based on result type and number of fields
        if (resultType == Map.class) {
            // Return as Map<String, Object>
            return executeMapProjection();
        } else if (projections.size() == 1 && isSimpleType(resultType)) {
            // Single field projection to simple type
            return executeSingleFieldProjection();
        } else {
            // Multiple field projection to bean
            return executeBeanProjection();
        }
    }
    
    /**
     * Execute projection query and return first result
     */
    public R first() {
        log.debug("Executing projection query to get first result");
        List<R> results = toList();
        if (results.isEmpty()) {
            throw new RuntimeException("No elements found");
        }
        return results.get(0);
    }
    
    /**
     * Execute projection query and return first result or null
     */
    public R firstOrDefault() {
        log.debug("Executing projection query to get first result or default");
        List<R> results = toList();
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Execute projection query with pagination
     */
    public Page<R> toPage(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }

        log.debug("Executing projection query with pagination");

        if (projections.isEmpty()) {
            throw new IllegalStateException("No fields specified for projection");
        }

        // Get total count first
        long totalCount = queryBuilder.count();

        List<R> content;
        if (resultType == Map.class) {
            content = executeMapProjectionWithPagination(pageable);
        } else if (projections.size() == 1 && isSimpleType(resultType)) {
            content = executeSingleFieldProjectionWithPagination(pageable);
        } else {
            content = executeBeanProjectionWithPagination(pageable);
        }

        return new PageImpl<>(content, pageable, totalCount);
    }

    /**
     * Execute single field projection
     */
    @SuppressWarnings("unchecked")
    private List<R> executeSingleFieldProjection() {
        CriteriaQuery<R> query = (CriteriaQuery<R>) criteriaBuilder.createQuery(resultType);
        Root<T> root = query.from(entityClass);
        query.select((Selection<? extends R>) projections.get(0));

        // Apply where conditions from the original query builder
        addWhereConditions(query, root);

        TypedQuery<R> typedQuery = entityManager.createQuery(query);
        List<R> results = typedQuery.getResultList();
        log.debug("Single field projection query returned {} results", results.size());
        return results;
    }

    /**
     * Execute bean projection (simplified - returns Object[] for now)
     */
    @SuppressWarnings("unchecked")
    private List<R> executeBeanProjection() {
        // For now, return as Object[] - full bean projection would require more complex mapping
        CriteriaQuery<Object[]> query = criteriaBuilder.createQuery(Object[].class);
        Root<T> root = query.from(entityClass);
        query.multiselect(projections);

        // Apply where conditions from the original query builder
        addWhereConditions(query, root);

        TypedQuery<Object[]> typedQuery = entityManager.createQuery(query);
        List<Object[]> arrayResults = typedQuery.getResultList();

        // Convert to result type (simplified)
        List<R> results = new ArrayList<>();
        for (Object[] row : arrayResults) {
            results.add((R) row);
        }

        log.debug("Bean projection query returned {} results", results.size());
        return results;
    }

    /**
     * Execute map projection
     */
    @SuppressWarnings("unchecked")
    private List<R> executeMapProjection() {
        CriteriaQuery<Object[]> query = criteriaBuilder.createQuery(Object[].class);
        Root<T> root = query.from(entityClass);
        query.multiselect(projections);

        // Apply where conditions from the original query builder
        addWhereConditions(query, root);

        TypedQuery<Object[]> typedQuery = entityManager.createQuery(query);
        List<Object[]> arrayResults = typedQuery.getResultList();

        List<R> results = new ArrayList<>();
        for (Object[] row : arrayResults) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < fieldNames.size() && i < row.length; i++) {
                map.put(fieldNames.get(i), row[i]);
            }
            results.add((R) map);
        }

        log.debug("Map projection query returned {} results", results.size());
        return results;
    }

    /**
     * Execute single field projection with pagination
     */
    @SuppressWarnings("unchecked")
    private List<R> executeSingleFieldProjectionWithPagination(Pageable pageable) {
        CriteriaQuery<R> query = (CriteriaQuery<R>) criteriaBuilder.createQuery(resultType);
        Root<T> root = query.from(entityClass);
        query.select((Selection<? extends R>) projections.get(0));

        addWhereConditions(query, root);
        addOrdering(query, root, pageable);

        TypedQuery<R> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return typedQuery.getResultList();
    }

    /**
     * Execute bean projection with pagination
     */
    @SuppressWarnings("unchecked")
    private List<R> executeBeanProjectionWithPagination(Pageable pageable) {
        CriteriaQuery<Object[]> query = criteriaBuilder.createQuery(Object[].class);
        Root<T> root = query.from(entityClass);
        query.multiselect(projections);

        addWhereConditions(query, root);
        addOrdering(query, root, pageable);

        TypedQuery<Object[]> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Object[]> arrayResults = typedQuery.getResultList();
        List<R> results = new ArrayList<>();
        for (Object[] row : arrayResults) {
            results.add((R) row);
        }
        return results;
    }

    /**
     * Execute map projection with pagination
     */
    @SuppressWarnings("unchecked")
    private List<R> executeMapProjectionWithPagination(Pageable pageable) {
        CriteriaQuery<Object[]> query = criteriaBuilder.createQuery(Object[].class);
        Root<T> root = query.from(entityClass);
        query.multiselect(projections);

        addWhereConditions(query, root);
        addOrdering(query, root, pageable);

        TypedQuery<Object[]> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Object[]> arrayResults = typedQuery.getResultList();
        List<R> results = new ArrayList<>();
        for (Object[] row : arrayResults) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < fieldNames.size() && i < row.length; i++) {
                map.put(fieldNames.get(i), row[i]);
            }
            results.add((R) map);
        }
        return results;
    }

    /**
     * Add where conditions from the query builder to the criteria query
     */
    private void addWhereConditions(CriteriaQuery<?> query, Root<T> root) {
        // This is a simplified implementation - in a full implementation,
        // we would need to extract and apply the where conditions from the queryBuilder
        // For now, we'll leave this as a placeholder
    }

    /**
     * Add ordering from pageable to the criteria query
     */
    private void addOrdering(CriteriaQuery<?> query, Root<T> root, Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            query.orderBy(pageable.getSort().stream()
                .map(order -> order.isAscending() ?
                    criteriaBuilder.asc(root.get(order.getProperty())) :
                    criteriaBuilder.desc(root.get(order.getProperty())))
                .toArray(jakarta.persistence.criteria.Order[]::new));
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
     * Check if type is a simple type (String, Number, etc.)
     */
    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() ||
               type == String.class ||
               Number.class.isAssignableFrom(type) ||
               type == Boolean.class ||
               type.isEnum();
    }
}