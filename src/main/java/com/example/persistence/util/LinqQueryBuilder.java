package com.example.persistence.util;

import com.example.persistence.dto.FilterCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * LINQ-style query builder for fluent query construction using HQL (Hibernate Query Language)
 */
@Slf4j
public class LinqQueryBuilder<T> {

    private final Class<T> entityClass;
    private final EntityManager entityManager;
    private final String entityName;
    private final List<String> whereConditions;
    private final List<String> orConditions;
    private final List<String> orderByList;
    private final List<String> selectFields;
    private final List<String> groupByFields;
    private Integer limitValue;
    private Integer offsetValue;

    public LinqQueryBuilder(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.entityName = entityClass.getSimpleName();
        this.whereConditions = new ArrayList<>();
        this.orConditions = new ArrayList<>();
        this.orderByList = new ArrayList<>();
        this.selectFields = new ArrayList<>();
        this.groupByFields = new ArrayList<>();
        log.debug("Created LinqQueryBuilder for entity: {}", entityName);
    }

    /**
     * Add WHERE condition (equivalent to LINQ Where)
     */
    public LinqQueryBuilder<T> where(String fieldName, Object value) {
        return where(fieldName, FilterCriteria.FilterOperator.EQUALS, value);
    }

    /**
     * Add WHERE condition with operator
     */
    public LinqQueryBuilder<T> where(String fieldName, FilterCriteria.FilterOperator operator, Object value) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }

        try {
            String hqlField = QueryUtils.convertToHqlField(fieldName);
            String condition = QueryUtils.buildCondition(hqlField, operator, value);
            if (condition != null) {
                whereConditions.add(condition);
                log.trace("Added WHERE condition: {} {} {}", fieldName, operator, value);
            }
        } catch (Exception e) {
            log.error("Failed to add WHERE condition: {} {} {}", fieldName, operator, value, e);
            throw new RuntimeException("Failed to add WHERE condition: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Add WHERE condition using lambda expression (equivalent to LINQ Where with lambda)
     * Example: where(User::getEmail, FilterCriteria.FilterOperator.EQUALS, "test@example.com")
     */
    public <R> LinqQueryBuilder<T> where(Function<T, R> fieldSelector, FilterCriteria.FilterOperator operator, Object value) {
        String fieldName = extractFieldNameFromMethodReference(fieldSelector);
        return where(fieldName, operator, value);
    }

    /**
     * Add WHERE condition using lambda expression with default EQUALS operator
     * Example: where(User::getEmail, "test@example.com")
     */
    public <R> LinqQueryBuilder<T> where(Function<T, R> fieldSelector, Object value) {
        return where(fieldSelector, FilterCriteria.FilterOperator.EQUALS, value);
    }

    /**
     * Add OR WHERE condition
     */
    public LinqQueryBuilder<T> orWhere(String fieldName, FilterCriteria.FilterOperator operator, Object value) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }

        try {
            String hqlField = QueryUtils.convertToHqlField(fieldName);
            String condition = QueryUtils.buildCondition(hqlField, operator, value);
            if (condition != null) {
                orConditions.add(condition);
                log.trace("Added OR WHERE condition: {} {} {}", fieldName, operator, value);
            }
        } catch (Exception e) {
            log.error("Failed to add OR WHERE condition: {} {} {}", fieldName, operator, value, e);
            throw new RuntimeException("Failed to add OR WHERE condition: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Add ORDER BY ascending (equivalent to LINQ OrderBy)
     */
    public LinqQueryBuilder<T> orderBy(String fieldName) {
        return orderBy(fieldName, FilterCriteria.SortDirection.ASC);
    }

    /**
     * Add ORDER BY with direction
     */
    public LinqQueryBuilder<T> orderBy(String fieldName, FilterCriteria.SortDirection direction) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (direction == null) {
            direction =  FilterCriteria.SortDirection.ASC;
        }

        try {
            String hqlField = QueryUtils.convertToHqlField(fieldName);
            String orderClause = direction ==  FilterCriteria.SortDirection.DESC ?
                hqlField + " DESC" : hqlField + " ASC";
            orderByList.add(orderClause);
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
        return orderBy(fieldName, FilterCriteria.SortDirection.DESC);
    }

    /**
     * Add ORDER BY ascending using lambda expression (equivalent to LINQ OrderBy with lambda)
     * Example: orderBy(User::getEmail)
     */
    public <R> LinqQueryBuilder<T> orderBy(Function<T, R> fieldSelector) {
        String fieldName = extractFieldNameFromMethodReference(fieldSelector);
        return orderBy(fieldName, FilterCriteria.SortDirection.ASC);
    }

    /**
     * Add ORDER BY with direction using lambda expression
     * Example: orderBy(User::getEmail, FilterCriteria.SortDirection.DESC)
     */
    public <R> LinqQueryBuilder<T> orderBy(Function<T, R> fieldSelector, FilterCriteria.SortDirection direction) {
        String fieldName = extractFieldNameFromMethodReference(fieldSelector);
        return orderBy(fieldName, direction);
    }

    /**
     * Add ORDER BY descending using lambda expression (equivalent to LINQ OrderByDescending with lambda)
     * Example: orderByDescending(User::getCreatedAt)
     */
    public <R> LinqQueryBuilder<T> orderByDescending(Function<T, R> fieldSelector) {
        String fieldName = extractFieldNameFromMethodReference(fieldSelector);
        return orderBy(fieldName, FilterCriteria.SortDirection.DESC);
    }

    /**
     * Set LIMIT for query results
     */
    public LinqQueryBuilder<T> limit(int limit) {
        this.limitValue = limit;
        log.trace("Set LIMIT: {}", limit);
        return this;
    }

    /**
     * Set OFFSET for query results
     */
    public LinqQueryBuilder<T> offset(int offset) {
        this.offsetValue = offset;
        log.trace("Set OFFSET: {}", offset);
        return this;
    }

    /**
     * Add SELECT clause to specify which fields to return (equivalent to LINQ Select)
     */
    public LinqQueryBuilder<T> select(String... fields) {
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("Select fields cannot be null or empty");
        }

        try {
            for (String field : fields) {
                if (field != null && !field.trim().isEmpty()) {
                    String hqlField = QueryUtils.convertToHqlField(field);
                    selectFields.add(hqlField);
                    log.trace("Added SELECT field: {}", field);
                }
            }
        } catch (Exception e) {
            log.error("Failed to add SELECT fields: {}", Arrays.toString(fields), e);
            throw new RuntimeException("Failed to add SELECT fields: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Add SELECT clause using lambda expressions (equivalent to LINQ Select with lambda)
     * Example: select(User::getEmail, User::getFullName)
     */
    @SafeVarargs
    public final LinqQueryBuilder<T> select(Function<T, ?>... fieldSelectors) {
        if (fieldSelectors == null || fieldSelectors.length == 0) {
            throw new IllegalArgumentException("Select field selectors cannot be null or empty");
        }

        try {
            for (Function<T, ?> fieldSelector : fieldSelectors) {
                String fieldName = extractFieldNameFromMethodReference(fieldSelector);
                String hqlField = QueryUtils.convertToHqlField(fieldName);
                selectFields.add(hqlField);
                log.trace("Added SELECT field via lambda: {}", fieldName);
            }
        } catch (Exception e) {
            log.error("Failed to add SELECT fields via lambda: {}", Arrays.toString(fieldSelectors), e);
            throw new RuntimeException("Failed to add SELECT fields via lambda: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Add GROUP BY clause (equivalent to LINQ GroupBy)
     */
    public LinqQueryBuilder<T> groupBy(String... fields) {
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("GroupBy fields cannot be null or empty");
        }

        try {
            for (String field : fields) {
                if (field != null && !field.trim().isEmpty()) {
                    String hqlField = QueryUtils.convertToHqlField(field);
                    groupByFields.add(hqlField);
                    log.trace("Added GROUP BY field: {}", field);
                }
            }
        } catch (Exception e) {
            log.error("Failed to add GROUP BY fields: {}", Arrays.toString(fields), e);
            throw new RuntimeException("Failed to add GROUP BY fields: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Execute query and return list of results
     */
    public List<T> toList() {
        log.debug("Executing query and returning list of results");
        try {
            String hql = buildHqlQuery();
            
            if (!selectFields.isEmpty()) {
                // When using SELECT with specific fields, return Object[] list
                @SuppressWarnings("unchecked")
                List<Object[]> results = entityManager.createQuery(hql).getResultList();
                log.debug("Query executed successfully, found {} results (select fields)", results.size());
                
                // For backward compatibility, we'll still return List<T> but this may cause ClassCastException
                // In practice, users should use toObjectList() when using select()
                @SuppressWarnings("unchecked")
                List<T> castResults = (List<T>) results;
                return castResults;
            } else {
                TypedQuery<T> query = entityManager.createQuery(hql, entityClass);
                applyQueryParameters(query);
                applyPagination(query);
                
                List<T> results = query.getResultList();
                log.debug("Query executed successfully, found {} results", results.size());
                return results;
            }
        } catch (Exception e) {
            log.error("Failed to execute query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * Execute query and return list of Object[] results (for SELECT with specific fields)
     */
    public List<Object[]> toObjectList() {
        log.debug("Executing query and returning list of Object[] results");
        try {
            String hql = buildHqlQuery();
            jakarta.persistence.Query query = entityManager.createQuery(hql);
            applyQueryParameters(query);
            applyPagination(query);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            log.debug("Query executed successfully, found {} Object[] results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to execute query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * Execute query and return first Object[] result or null (for SELECT with specific fields)
     */
    public Object[] firstOrDefaultObject() {
        log.debug("Executing query and returning first Object[] result or null");
        try {
            String hql = buildHqlQuery();
            jakarta.persistence.Query query = entityManager.createQuery(hql);
            applyQueryParameters(query);
            applyPagination(query);
            query.setMaxResults(1);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            return results.isEmpty() ? null : results.getFirst();
        } catch (Exception e) {
            log.error("Failed to execute firstOrDefaultObject query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute firstOrDefaultObject query: " + e.getMessage(), e);
        }
    }

    /**
     * Execute query and return first result or null
     */
    public T firstOrDefault() {
        log.debug("Executing query and returning first result or null");
        try {
            String hql = buildHqlQuery();
            TypedQuery<T> query = entityManager.createQuery(hql, entityClass);
            applyQueryParameters(query);
            applyPagination(query);
            query.setMaxResults(1);
            
            List<T> results = query.getResultList();
            return results.isEmpty() ? null : results.getFirst();
        } catch (Exception e) {
            log.error("Failed to execute firstOrDefault query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute firstOrDefault query: " + e.getMessage(), e);
        }
    }

    /**
     * Check if any results exist
     */
    public boolean any() {
        log.debug("Checking if any results exist");
        try {
            String hql = buildHqlQuery();
            TypedQuery<T> query = entityManager.createQuery(hql, entityClass);
            applyQueryParameters(query);
            query.setMaxResults(1);
            
            List<T> results = query.getResultList();
            return !results.isEmpty();
        } catch (Exception e) {
            log.error("Failed to execute any query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute any query: " + e.getMessage(), e);
        }
    }

    /**
     * Execute query with pagination and return Page
     */
    public Page<T> toPage(Pageable pageable) {
        log.debug("Executing query with pagination: {}", pageable);
        try {
            // Get total count
            long total = count();
            
            // Get page content
            String hql = buildHqlQuery();
            TypedQuery<T> query = entityManager.createQuery(hql, entityClass);
            applyQueryParameters(query);
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
            
            List<T> content = query.getResultList();
            
            return new PageImpl<>(content, pageable, total);
        } catch (Exception e) {
            log.error("Failed to execute paginated query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute paginated query: " + e.getMessage(), e);
        }
    }

    /**
     * Get count of results
     */
    public long count() {
        log.debug("Getting count of results");
        try {
            StringBuilder countHql = new StringBuilder("SELECT COUNT(*) FROM ").append(entityName);
            String whereClause = buildWhereClause();
            countHql.append(whereClause);
            
            TypedQuery<Long> query = entityManager.createQuery(countHql.toString(), Long.class);
            applyQueryParameters(query);
            
            return query.getSingleResult();
        } catch (Exception e) {
            log.error("Failed to execute count query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute count query: " + e.getMessage(), e);
        }
    }

    /**
     * Build complete HQL query
     */
    private String buildHqlQuery() {
        StringBuilder hql = new StringBuilder();
        
        // Add SELECT clause if fields are specified
        if (!selectFields.isEmpty()) {
            hql.append("SELECT ").append(String.join(", ", selectFields)).append(" ");
        } else {
            hql.append("SELECT ").append(entityName).append(" ");
        }
        
        hql.append("FROM ").append(entityName);
        hql.append(buildWhereClause());
        
        // Add GROUP BY clause if fields are specified
        if (!groupByFields.isEmpty()) {
            hql.append(" GROUP BY ").append(String.join(", ", groupByFields));
        }
        
        if (!orderByList.isEmpty()) {
            hql.append(" ORDER BY ").append(String.join(", ", orderByList));
        }
        
        log.debug("Built HQL query: {}", hql);
        return hql.toString();
    }

    /**
     * Build WHERE clause from conditions
     */
    private String buildWhereClause() {
        if (whereConditions.isEmpty() && orConditions.isEmpty()) {
            return "";
        }

        StringBuilder whereClause = new StringBuilder(" WHERE ");
        
        if (!whereConditions.isEmpty()) {
            whereClause.append(String.join(" AND ", whereConditions));
        }
        
        if (!orConditions.isEmpty()) {
            if (!whereConditions.isEmpty()) {
                whereClause.append(" AND ");
            }
            whereClause.append("(").append(String.join(" OR ", orConditions)).append(")");
        }
        
        return whereClause.toString();
    }

    /**
     * Apply query parameters to the query
     */
    private void applyQueryParameters(jakarta.persistence.Query query) {
        // This is a simplified implementation - in a real scenario,
        // you would need to track and set parameters properly
        log.trace("Applying query parameters (simplified implementation)");
    }

    /**
     * Apply pagination to the query
     */
    private void applyPagination(jakarta.persistence.Query query) {
        if (offsetValue != null) {
            query.setFirstResult(offsetValue);
        }
        if (limitValue != null) {
            query.setMaxResults(limitValue);
        }
    }

    /**
     * Extract field name from method reference (e.g., User::getEmail -> "email")
     */
    private <R> String extractFieldNameFromMethodReference(Function<T, R> fieldSelector) {
        try {
            // This is a simplified approach - in a real implementation, you might want to use
            // a more robust method like SerializedLambda or other techniques
            // For now, we'll use a simple approach that works for basic getter methods
            
            // Get the method name from the function's toString()
            String methodRef = fieldSelector.toString();
            
            // Extract method name from typical method reference format
            // Example: "com.example.persistence.entity.User::getEmail" -> "getEmail"
            int lastColon = methodRef.lastIndexOf("::");
            if (lastColon != -1 && lastColon + 2 < methodRef.length()) {
                String methodName = methodRef.substring(lastColon + 2);
                
                // Convert getter method name to field name
                if (methodName.startsWith("get")) {
                    return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                } else if (methodName.startsWith("is")) {
                    return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                }
                return methodName;
            }
            
            throw new IllegalArgumentException("Could not extract field name from method reference: " + methodRef);
        } catch (Exception e) {
            log.error("Failed to extract field name from method reference", e);
            throw new RuntimeException("Failed to extract field name from method reference: " + e.getMessage(), e);
        }
    }
}
