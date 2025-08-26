package com.example.persistence.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for QueryUtils class
 */
class QueryUtilsTest {

    @Test
    void testFormatLikeCondition() {
        // Test basic LIKE condition
        String result = QueryUtils.formatLikeCondition("username", "test", true, true, false);
        assertThat(result).isEqualTo("username LIKE :username");
        
        // Test NOT LIKE condition
        String notLikeResult = QueryUtils.formatLikeCondition("email", "example", true, true, true);
        assertThat(notLikeResult).isEqualTo("email NOT LIKE :email");
        
        // Test STARTS_WITH pattern (no prefix wildcard, suffix wildcard)
        String startsWithResult = QueryUtils.formatLikeCondition("name", "John", false, true, false);
        assertThat(startsWithResult).isEqualTo("name LIKE :name");
        
        // Test ENDS_WITH pattern (prefix wildcard, no suffix wildcard)
        String endsWithResult = QueryUtils.formatLikeCondition("domain", "com", true, false, false);
        assertThat(endsWithResult).isEqualTo("domain LIKE :domain");
    }

    @Test
    void testFormatLikeConditionWithNullValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtils.formatLikeCondition("field", null, true, true, false);
        });
    }

    @Test
    void testSanitizeParameterName() {
        assertThat(QueryUtils.sanitizeParameterName("user.name")).isEqualTo("user_name");
        assertThat(QueryUtils.sanitizeParameterName("user-name")).isEqualTo("user_name");
        assertThat(QueryUtils.sanitizeParameterName("user@email")).isEqualTo("user_email");
        assertThat(QueryUtils.sanitizeParameterName("normalField")).isEqualTo("normalField");
    }

    @Test
    void testConvertToHqlField() {
        assertThat(QueryUtils.convertToHqlField("userName")).isEqualTo("userName");
        assertThat(QueryUtils.convertToHqlField("user.name")).isEqualTo("user.name");
    }

    @Test
    void testConvertToHqlFieldWithInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtils.convertToHqlField(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtils.convertToHqlField("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtils.convertToHqlField("   ");
        });
    }
}
