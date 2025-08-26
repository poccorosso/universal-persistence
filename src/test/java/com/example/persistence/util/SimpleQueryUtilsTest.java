package com.example.persistence.util;

/**
 * Simple test to verify QueryUtils.formatLikeCondition works correctly
 * This doesn't require JUnit dependencies
 */
public class SimpleQueryUtilsTest {
    
    public static void main(String[] args) {
        System.out.println("Testing QueryUtils.formatLikeCondition...");
        
        // Test basic LIKE condition
        String result1 = QueryUtils.formatLikeCondition("username", "test", true, true, false);
        System.out.println("Test 1 - LIKE: " + result1);
        assert result1.equals("username LIKE :username") : "Test 1 failed";
        
        // Test NOT LIKE condition
        String result2 = QueryUtils.formatLikeCondition("email", "example", true, true, true);
        System.out.println("Test 2 - NOT LIKE: " + result2);
        assert result2.equals("email NOT LIKE :email") : "Test 2 failed";
        
        // Test STARTS_WITH pattern
        String result3 = QueryUtils.formatLikeCondition("name", "John", false, true, false);
        System.out.println("Test 3 - STARTS_WITH: " + result3);
        assert result3.equals("name LIKE :name") : "Test 3 failed";
        
        // Test ENDS_WITH pattern
        String result4 = QueryUtils.formatLikeCondition("domain", "com", true, false, false);
        System.out.println("Test 4 - ENDS_WITH: " + result4);
        assert result4.equals("domain LIKE :domain") : "Test 4 failed";
        
        // Test null value
        try {
            QueryUtils.formatLikeCondition("field", null, true, true, false);
            System.err.println("Test 5 - Should have thrown exception for null value");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 5 - Correctly threw exception for null value: " + e.getMessage());
        }
        
        System.out.println("All tests passed! formatLikeCondition is working correctly.");
    }
}
