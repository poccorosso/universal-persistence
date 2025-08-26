package com.example.persistence.util;

import com.example.persistence.dto.FilterCriteria;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;

import java.util.*;

/**
 * Dynamic query builder with LINQ-style support using HQL (Hibernate Query Language)
 */
@Slf4j
public class QueryBuilder<T> {

    private final Class<T> entityClass;
    private final EntityManager entityManager;
    private final String entityName;

    public QueryBuilder(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.entityName = entityClass.getSimpleName();
        log.debug("Created QueryBuilder for entity: {}", entityName);
    }
    
    /**
     * Create LINQ-style query builder
     */
    public LinqQueryBuilder<T> linq() {
        log.debug("Creating LINQ-style query builder for entity: {}", entityName);
        return new LinqQueryBuilder<>(entityClass, entityManager);
    }

    /**
     * Build complete HQL query with WHERE clause
     */
    public String buildQuery(List<FilterCriteria> filters) {
        return buildQuery(filters, null, null);
    }

    /**
     * Build complete HQL query with WHERE clause and ORDER BY
     */
    public String buildQuery(List<FilterCriteria> filters, String orderByField, Sort.Direction direction) {
        StringBuilder query = new StringBuilder("FROM ").append(entityName);
        
        String whereClause = buildWhereClause(filters);
        query.append(whereClause);
        
        if (orderByField != null && !orderByField.trim().isEmpty()) {
            String hqlField = QueryUtils.convertToHqlField(orderByField);
            String orderDirection = direction == Sort.Direction.DESC ? "DESC" : "ASC";
            query.append(" ORDER BY ").append(hqlField).append(" ").append(orderDirection);
        }
        
        log.debug("Built query: {}", query);
        return query.toString();
    }

    /**
     * Build count query with WHERE clause
     */
    public String buildCountQuery(List<FilterCriteria> filters) {
        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM ").append(entityName);
        
        String whereClause = buildWhereClause(filters);
        query.append(whereClause);
        
        log.debug("Built count query: {}", query);
        return query.toString();
    }

    /**
     * Execute query and return results
     */
    public List<T> executeQuery(List<FilterCriteria> filters) {
        return executeQuery(filters, null, null);
    }

    /**
     * Execute query with ordering and return results
     */
    public List<T> executeQuery(List<FilterCriteria> filters, String orderByField, Sort.Direction direction) {
        String queryString = buildQuery(filters, orderByField, direction);
        jakarta.persistence.Query query = entityManager.createQuery(queryString);
        
        setQueryParameters(query, filters);
        
        @SuppressWarnings("unchecked")
        List<T> result = query.getResultList();
        log.debug("Executed query, found {} results", result.size());
        return result;
    }

    /**
     * Execute query with ordering and pagination
     */
    public List<T> executeQuery(List<FilterCriteria> filters, String orderByField, Sort.Direction direction, int offset, int limit) {
        String queryString = buildQuery(filters, orderByField, direction);
        jakarta.persistence.Query query = entityManager.createQuery(queryString);
        
        setQueryParameters(query, filters);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        
        @SuppressWarnings("unchecked")
        List<T> result = query.getResultList();
        log.debug("Executed query with pagination, found {} results (offset: {}, limit: {})", result.size(), offset, limit);
        return result;
    }

    /**
     * Execute count query and return result
     */
    public long executeCountQuery(List<FilterCriteria> filters) {
        String queryString = buildCountQuery(filters);
        jakarta.persistence.Query query = entityManager.createQuery(queryString);
        
        setQueryParameters(query, filters);
        
        Long result = (Long) query.getSingleResult();
        log.debug("Executed count query, result: {}", result);
        return result != null ? result : 0L;
    }

    /**
     * Set query parameters based on filter criteria
     */
    private void setQueryParameters(jakarta.persistence.Query query, List<FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        
        for (FilterCriteria filter : filters) {
            String field = filter.getField();
            Object value = filter.getValue();
            String hqlField = QueryUtils.convertToHqlField(field);
            
            if (value != null) {
                switch (filter.getOperator()) {
                    case EQUALS:
                    case NOT_EQUALS:
                    case GREATER_THAN:
                    case GREATER_EQUAL:
                    case LESS_THAN:
                    case LESS_EQUAL:
                        query.setParameter(QueryUtils.sanitizeParameterName(hqlField), value);
                        break;
                    case LIKE:
                    case NOT_LIKE:
                    case STARTS_WITH:
                    case ENDS_WITH:
                    case CONTAINS:
                        String pattern;
                        if (filter.getOperator() == FilterCriteria.FilterOperator.STARTS_WITH) {
                            pattern = value.toString() + "%";
                        } else if (filter.getOperator() == FilterCriteria.FilterOperator.ENDS_WITH) {
                            pattern = "%" + value.toString();
                        } else {
                            pattern = "%" + value.toString() + "%";
                        }
                        query.setParameter(QueryUtils.sanitizeParameterName(hqlField), pattern);
                        break;
                    case IN:
                    case NOT_IN:
                        query.setParameter(QueryUtils.sanitizeParameterName(hqlField), value);
                        break;
                    case BETWEEN:
                        Collection<?> values;
                        if (value.getClass().isArray()) {
                            values = Arrays.asList((Object[]) value);
                        } else {
                            values = (Collection<?>) value;
                        }
                        if (values.size() == 2) {
                            Object[] array = values.toArray();
                            query.setParameter(QueryUtils.sanitizeParameterName(hqlField + "_from"), array[0]);
                            query.setParameter(QueryUtils.sanitizeParameterName(hqlField + "_to"), array[1]);
                        }
                        break;
                    case IS_NULL:
                    case IS_NOT_NULL:
                        // No parameters to set for NULL checks
                        break;
                }
            }
        }
    }

    public String buildWhereClause(List<FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            log.debug("No filters provided, returning empty where clause");
            return "";
        }

        log.debug("Building where clause with {} filters", filters.size());
        
        List<String> whereConditions = new ArrayList<>();
        List<String> orConditions = new ArrayList<>();

        for (FilterCriteria filter : filters) {
            try {
                String condition = buildSingleCondition(filter);
                if (condition != null && !condition.isEmpty()) {
                    if (filter.getLogicalOperator() == FilterCriteria.LogicalOperator.OR) {
                        orConditions.add(condition);
                        log.trace("Added OR condition for field: {}", filter.getField());
                    } else {
                        whereConditions.add(condition);
                        log.trace("Added AND condition for field: {}", filter.getField());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to build condition for filter: {}", filter, e);
                throw e;
            }
        }

        // Combine conditions
        StringBuilder whereClause = new StringBuilder();
        boolean hasWhereConditions = !whereConditions.isEmpty();
        boolean hasOrConditions = !orConditions.isEmpty();

        if (hasWhereConditions || hasOrConditions) {
            whereClause.append(" WHERE ");
            
            List<String> allConditions = new ArrayList<>();
            if (hasWhereConditions) {
                allConditions.add(String.join(" AND ", whereConditions));
            }
            if (hasOrConditions) {
                String orClause = "(" + String.join(" OR ", orConditions) + ")";
                allConditions.add(orClause);
            }
            
            whereClause.append(String.join(" AND ", allConditions));
        }

        return whereClause.toString();
    }

    private String buildSingleCondition(FilterCriteria filter) {
        try {
            String field = filter.getField();
            Object value = filter.getValue();
            
            log.trace("Building condition: {} {} {}", field, filter.getOperator(), value);

            // Handle nested field paths (e.g., "user.name")
            String hqlField = QueryUtils.convertToHqlField(field);

            return switch (filter.getOperator()) {
                case EQUALS -> QueryUtils.formatCondition(hqlField, "=", value);
                case NOT_EQUALS -> QueryUtils.formatCondition(hqlField, "<>", value);
                case GREATER_THAN -> QueryUtils.formatCondition(hqlField, ">", value);
                case GREATER_EQUAL -> QueryUtils.formatCondition(hqlField, ">=", value);
                case LESS_THAN -> QueryUtils.formatCondition(hqlField, "<", value);
                case LESS_EQUAL -> QueryUtils.formatCondition(hqlField, "<=", value);
                case LIKE, CONTAINS -> QueryUtils.formatLikeCondition(hqlField, value, true, true, false);
                case NOT_LIKE -> QueryUtils.formatLikeCondition(hqlField, value, true, true, true);
                case STARTS_WITH -> QueryUtils.formatLikeCondition(hqlField, value, false, true, false);
                case ENDS_WITH -> QueryUtils.formatLikeCondition(hqlField, value, true, false, false);
                case IN -> QueryUtils.formatInCondition(hqlField, value, false);
                case NOT_IN -> QueryUtils.formatInCondition(hqlField, value, true);
                case IS_NULL -> hqlField + " IS NULL";
                case IS_NOT_NULL -> hqlField + " IS NOT NULL";
                case BETWEEN -> QueryUtils.formatBetweenCondition(hqlField, value);
                default -> throw new IllegalArgumentException("Unsupported operator: " + filter.getOperator());
            };
        } catch (Exception e) {
            log.error("Failed to build condition for filter: {}", filter, e);
            throw new RuntimeException("Failed to build condition: " + e.getMessage(), e);
        }
    }
}
