package com.example.persistence.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for GROUP BY operations (equivalent to LINQ GroupBy) using JPA CriteriaBuilder
 */
@Slf4j
public class LinqGroupBuilder<T> {

    private final LinqQueryBuilder<T> queryBuilder;
    private final String groupByField;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final Class<T> entityClass;

    public LinqGroupBuilder(LinqQueryBuilder<T> queryBuilder, String groupByField) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException("QueryBuilder cannot be null");
        }
        if (groupByField == null || groupByField.trim().isEmpty()) {
            throw new IllegalArgumentException("GroupBy field cannot be null or empty");
        }
        this.queryBuilder = queryBuilder;
        this.groupByField = groupByField;
        this.entityManager = queryBuilder.getEntityManager();
        this.criteriaBuilder = queryBuilder.getCriteriaBuilder();
        this.entityClass = getEntityClassFromQueryBuilder(queryBuilder);
    }

    @SuppressWarnings("unchecked")
    private Class<T> getEntityClassFromQueryBuilder(LinqQueryBuilder<T> queryBuilder) {
        try {
            java.lang.reflect.Field field = queryBuilder.getClass().getDeclaredField("entityClass");
            field.setAccessible(true);
            return (Class<T>) field.get(queryBuilder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get entity class from query builder", e);
        }
    }

    /**
     * Execute group by query and return map of groups
     * Note: This is a simplified implementation that groups results in memory
     */
    public Map<Object, List<T>> toMap() {
        log.debug("Executing group by query: {}", groupByField);

        try {
            // Get all entities first
            CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            query.select(root);

            // Apply where conditions from the original query builder (simplified)
            // In a full implementation, we would extract conditions from queryBuilder

            TypedQuery<T> typedQuery = entityManager.createQuery(query);
            List<T> allResults = typedQuery.getResultList();

            // Group results in memory by the specified field
            Map<Object, List<T>> groupedResults = new HashMap<>();
            for (T entity : allResults) {
                Object groupKey = getFieldValue(entity, groupByField);
                groupedResults.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(entity);
            }

            log.debug("Group by query returned {} groups", groupedResults.size());
            return groupedResults;

        } catch (Exception e) {
            log.error("Failed to execute group by query", e);
            throw new RuntimeException("Failed to execute group by query", e);
        }
    }

    /**
     * Count items in each group
     */
    public Map<Object, Long> count() {
        log.debug("Executing group by count query: {}", groupByField);

        try {
            // Use CriteriaBuilder to create a proper GROUP BY query
            CriteriaQuery<Object[]> query = criteriaBuilder.createQuery(Object[].class);
            Root<T> root = query.from(entityClass);

            Path<?> groupPath = getFieldPath(root, groupByField);
            query.multiselect(groupPath, criteriaBuilder.count(root));
            query.groupBy(groupPath);

            TypedQuery<Object[]> typedQuery = entityManager.createQuery(query);
            List<Object[]> results = typedQuery.getResultList();

            Map<Object, Long> countMap = new HashMap<>();
            for (Object[] result : results) {
                countMap.put(result[0], (Long) result[1]);
            }

            log.debug("Group by count query returned {} groups", countMap.size());
            return countMap;

        } catch (Exception e) {
            log.error("Failed to execute group by count query", e);
            throw new RuntimeException("Failed to execute group by count query", e);
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
     * Get field value from entity using reflection
     */
    private Object getFieldValue(T entity, String fieldName) {
        try {
            String[] fieldParts = fieldName.split("\\.");
            Object currentObject = entity;

            for (String fieldPart : fieldParts) {
                java.lang.reflect.Field field = currentObject.getClass().getDeclaredField(fieldPart);
                field.setAccessible(true);
                currentObject = field.get(currentObject);
                if (currentObject == null) {
                    break;
                }
            }

            return currentObject;
        } catch (Exception e) {
            log.error("Failed to get field value: {}", fieldName, e);
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }
}