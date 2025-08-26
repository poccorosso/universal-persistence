package com.example.persistence.util;

import com.example.persistence.dto.FilterCriteria;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;

/**
 * Utility class for query building operations shared between QueryBuilder and LinqQueryBuilder
 */
@Slf4j
public class QueryUtils {

    private QueryUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Build condition string for HQL
     */
    public static String buildCondition(String hqlField, FilterCriteria.FilterOperator operator, Object value) {
        return switch (operator) {
            case EQUALS -> hqlField + " = :" + sanitizeParameterName(hqlField);
            case NOT_EQUALS -> hqlField + " != :" + sanitizeParameterName(hqlField);
            case LIKE, STARTS_WITH, ENDS_WITH, CONTAINS -> hqlField + " LIKE :" + sanitizeParameterName(hqlField);
            case NOT_LIKE -> hqlField + " NOT LIKE :" + sanitizeParameterName(hqlField);
            case GREATER_THAN -> hqlField + " > :" + sanitizeParameterName(hqlField);
            case GREATER_EQUAL -> hqlField + " >= :" + sanitizeParameterName(hqlField);
            case LESS_THAN -> hqlField + " < :" + sanitizeParameterName(hqlField);
            case LESS_EQUAL -> hqlField + " <= :" + sanitizeParameterName(hqlField);
            case IN -> hqlField + " IN :" + sanitizeParameterName(hqlField);
            case NOT_IN -> hqlField + " NOT IN :" + sanitizeParameterName(hqlField);
            case IS_NULL -> hqlField + " IS NULL";
            case IS_NOT_NULL -> hqlField + " IS NOT NULL";
            case BETWEEN -> hqlField + " BETWEEN :" + sanitizeParameterName(hqlField + "_from") +
                    " AND :" + sanitizeParameterName(hqlField + "_to");
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    /**
     * Convert field name to HQL field format
     */
    public static String convertToHqlField(String field) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        return field;
    }

    /**
     * Sanitize parameter name for HQL
     */
    public static String sanitizeParameterName(String fieldName) {
        return fieldName.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Format LIKE condition with wildcards
     * Note: Pattern creation is handled separately in QueryBuilder.setQueryParameters
     */
    public static String formatLikeCondition(String hqlField, Object value, boolean prefixWildcard, boolean suffixWildcard, boolean negate) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null for LIKE condition");
        }
        
        String operator = negate ? " NOT LIKE " : " LIKE ";
        return hqlField + operator + ":" + sanitizeParameterName(hqlField);
    }

    /**
     * Format IN condition
     */
    public static String formatInCondition(String hqlField, Object value, boolean negate) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null for IN condition");
        }
        
        if (!(value instanceof Collection) && !value.getClass().isArray()) {
            throw new IllegalArgumentException("IN condition requires a collection or array");
        }
        
        String operator = negate ? " NOT IN " : " IN ";
        return hqlField + operator + ":" + sanitizeParameterName(hqlField);
    }

    /**
     * Format BETWEEN condition
     */
    public static String formatBetweenCondition(String hqlField, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null for BETWEEN condition");
        }
        
        if (!(value instanceof Collection) && !value.getClass().isArray()) {
            throw new IllegalArgumentException("BETWEEN condition requires a collection or array with exactly 2 values");
        }
        
        Collection<?> values;
        if (value.getClass().isArray()) {
            values = Arrays.asList((Object[]) value);
        } else {
            values = (Collection<?>) value;
        }
        
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN condition requires exactly 2 values");
        }
        
        return hqlField + " BETWEEN :" + sanitizeParameterName(hqlField + "_from") + 
               " AND :" + sanitizeParameterName(hqlField + "_to");
    }

    /**
     * Format basic condition
     */
    public static String formatCondition(String hqlField, String operator, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null for operator: " + operator);
        }
        return hqlField + " " + operator + " :" + sanitizeParameterName(hqlField);
    }
}
