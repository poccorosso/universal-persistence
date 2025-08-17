package com.example.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {
    
    /**
     * Field name
     */
    private String field;
    
    /**
     * Filter operator
     */
    private FilterOperator operator;
    
    /**
     * Filter value
     */
    private Object value;
    
    /**
     * Logical operator (AND/OR)
     */
    private LogicalOperator logicalOperator = LogicalOperator.AND;
    
    public enum FilterOperator {
        EQUALS,           // equals
        NOT_EQUALS,       // not equals
        GREATER_THAN,     // greater than
        GREATER_EQUAL,    // greater than or equal
        LESS_THAN,        // less than
        LESS_EQUAL,       // less than or equal
        LIKE,             // like pattern matching
        NOT_LIKE,         // not like pattern matching
        IN,               // in list
        NOT_IN,           // not in list
        IS_NULL,          // is null
        IS_NOT_NULL,      // is not null
        BETWEEN,          // between two values
        STARTS_WITH,      // starts with
        ENDS_WITH,        // ends with
        CONTAINS          // contains
    }
    
    public enum LogicalOperator {
        AND, OR
    }
}