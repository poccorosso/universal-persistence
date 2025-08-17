package com.example.persistence.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL Injection validation utility
 */
@Component
@Slf4j
public class SqlInjectionValidator {
    
    // Common SQL injection patterns
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("('|(\\-\\-)|(;)|(\\|)|(\\*)|(%))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(union|select|insert|delete|update|drop|create|alter|exec|execute)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(script|javascript|vbscript|onload|onerror|onclick)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\<|\\>)", Pattern.CASE_INSENSITIVE)
    );
    
    // Dangerous SQL keywords that should not appear in user input
    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "EXEC", "EXECUTE",
        "UNION", "SCRIPT", "JAVASCRIPT", "VBSCRIPT", "ONLOAD", "ONERROR", "ONCLICK"
    );
    
    // Allowed SQL keywords for SELECT queries
    private static final List<String> ALLOWED_KEYWORDS = Arrays.asList(
        "SELECT", "FROM", "WHERE", "ORDER", "GROUP", "HAVING", "LIMIT", "OFFSET",
        "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "AS", "AND", "OR", "NOT",
        "IN", "LIKE", "BETWEEN", "IS", "NULL", "COUNT", "SUM", "AVG", "MAX", "MIN",
        "DISTINCT", "ASC", "DESC", "BY"
    );
    
    /**
     * Validate SQL query for potential injection attacks
     */
    public boolean isValidSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            log.warn("Empty or null SQL query provided");
            return false;
        }
        
        String cleanSql = sql.trim().toUpperCase();
        
        // Check if it's a SELECT query (read-only)
        if (!cleanSql.startsWith("SELECT")) {
            log.warn("Non-SELECT query detected: {}", sql.substring(0, Math.min(50, sql.length())));
            return false;
        }
        
        // Check for dangerous patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sql).find()) {
                log.warn("Potential SQL injection pattern detected: {}", pattern.pattern());
                return false;
            }
        }
        
        // Check for dangerous keywords
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (cleanSql.contains(keyword)) {
                log.warn("Dangerous SQL keyword detected: {}", keyword);
                return false;
            }
        }
        
        // Additional validation for common injection techniques
        if (containsSqlInjectionTechniques(sql)) {
            return false;
        }
        
        log.debug("SQL query validation passed");
        return true;
    }
    
    /**
     * Check for common SQL injection techniques
     */
    private boolean containsSqlInjectionTechniques(String sql) {
        String lowerSql = sql.toLowerCase();
        
        // Check for comment-based injection
        if (lowerSql.contains("--") || lowerSql.contains("/*") || lowerSql.contains("*/")) {
            log.warn("SQL comment detected, potential injection attempt");
            return true;
        }
        
        // Check for union-based injection
        if (lowerSql.contains("union") && lowerSql.contains("select")) {
            log.warn("UNION SELECT detected, potential injection attempt");
            return true;
        }
        
        // Check for boolean-based injection
        if (lowerSql.contains("1=1") || lowerSql.contains("1 = 1") || 
            lowerSql.contains("'='") || lowerSql.contains("' = '")) {
            log.warn("Boolean-based injection pattern detected");
            return true;
        }
        
        // Check for time-based injection
        if (lowerSql.contains("sleep(") || lowerSql.contains("waitfor") || 
            lowerSql.contains("delay") || lowerSql.contains("benchmark(")) {
            log.warn("Time-based injection pattern detected");
            return true;
        }
        
        // Check for stacked queries
        if (lowerSql.contains(";") && !lowerSql.endsWith(";")) {
            log.warn("Stacked query detected, potential injection attempt");
            return true;
        }
        
        return false;
    }
    
    /**
     * Sanitize SQL input by removing potentially dangerous characters
     */
    public String sanitizeSql(String sql) {
        if (sql == null) {
            return null;
        }
        
        // Remove dangerous characters
        String sanitized = sql.replaceAll("[';\\-\\-]", "");
        
        // Remove multiple spaces
        sanitized = sanitized.replaceAll("\\s+", " ");
        
        return sanitized.trim();
    }
    
    /**
     * Validate parameter value for potential injection
     */
    public boolean isValidParameter(Object parameter) {
        if (parameter == null) {
            return true;
        }
        
        String paramStr = parameter.toString();
        
        // Check for script injection in parameters
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(paramStr).find()) {
                log.warn("Potential injection in parameter: {}", paramStr);
                return false;
            }
        }
        
        return true;
    }
}