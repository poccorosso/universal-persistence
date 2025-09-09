package com.example.persistence.util;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * Builder for complex WHERE conditions with lambda-like syntax using JPA CriteriaBuilder
 */
@Slf4j
public class WhereBuilder<T> {

    private final Root<T> root;
    private final CriteriaBuilder criteriaBuilder;

    public WhereBuilder(Root<T> root, CriteriaBuilder criteriaBuilder) {
        this.root = root;
        this.criteriaBuilder = criteriaBuilder;
    }

    /**
     * Create field accessor for building conditions
     */
    public FieldAccessor field(String fieldName) {
        return new FieldAccessor(getFieldPath(fieldName));
    }

    /**
     * Create AND condition
     */
    public Predicate and(Predicate... predicates) {
        return criteriaBuilder.and(predicates);
    }

    /**
     * Create OR condition
     */
    public Predicate or(Predicate... predicates) {
        return criteriaBuilder.or(predicates);
    }

    /**
     * Create NOT condition
     */
    public Predicate not(Predicate predicate) {
        return criteriaBuilder.not(predicate);
    }

    /**
     * Get field path by name
     */
    private Path<?> getFieldPath(String fieldName) {
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
     * Field accessor for building conditions
     */
    public class FieldAccessor {
        private final Path<?> path;

        public FieldAccessor(Path<?> path) {
            this.path = path;
        }

        @SuppressWarnings("unchecked")
        public Predicate eq(Object value) {
            return criteriaBuilder.equal(path, value);
        }

        @SuppressWarnings("unchecked")
        public Predicate ne(Object value) {
            return criteriaBuilder.notEqual(path, value);
        }

        @SuppressWarnings("unchecked")
        public Predicate gt(Comparable value) {
            return criteriaBuilder.greaterThan((Path<Comparable>) path, value);
        }

        @SuppressWarnings("unchecked")
        public Predicate goe(Comparable value) {
            return criteriaBuilder.greaterThanOrEqualTo((Path<Comparable>) path, value);
        }

        @SuppressWarnings("unchecked")
        public Predicate lt(Comparable value) {
            return criteriaBuilder.lessThan((Path<Comparable>) path, value);
        }

        @SuppressWarnings("unchecked")
        public Predicate loe(Comparable value) {
            return criteriaBuilder.lessThanOrEqualTo((Path<Comparable>) path, value);
        }

        @SuppressWarnings("unchecked")
        public Predicate like(String pattern) {
            return criteriaBuilder.like((Path<String>) path, pattern);
        }

        @SuppressWarnings("unchecked")
        public Predicate notLike(String pattern) {
            return criteriaBuilder.notLike((Path<String>) path, pattern);
        }

        @SuppressWarnings("unchecked")
        public Predicate contains(String value) {
            return criteriaBuilder.like((Path<String>) path, "%" + value + "%");
        }

        @SuppressWarnings("unchecked")
        public Predicate startsWith(String value) {
            return criteriaBuilder.like((Path<String>) path, value + "%");
        }

        @SuppressWarnings("unchecked")
        public Predicate endsWith(String value) {
            return criteriaBuilder.like((Path<String>) path, "%" + value);
        }

        @SuppressWarnings("unchecked")
        public Predicate in(Collection<?> values) {
            return path.in(values);
        }

        @SuppressWarnings("unchecked")
        public Predicate in(Object... values) {
            return path.in(values);
        }

        @SuppressWarnings("unchecked")
        public Predicate notIn(Collection<?> values) {
            return criteriaBuilder.not(path.in(values));
        }

        @SuppressWarnings("unchecked")
        public Predicate notIn(Object... values) {
            return criteriaBuilder.not(path.in(values));
        }

        @SuppressWarnings("unchecked")
        public Predicate isNull() {
            return criteriaBuilder.isNull(path);
        }

        @SuppressWarnings("unchecked")
        public Predicate isNotNull() {
            return criteriaBuilder.isNotNull(path);
        }

        @SuppressWarnings("unchecked")
        public Predicate between(Comparable from, Comparable to) {
            return criteriaBuilder.between((Path<Comparable>) path, from, to);
        }

        // String-specific methods
        @SuppressWarnings("unchecked")
        public Predicate isEmpty() {
            return criteriaBuilder.equal((Path<String>) path, "");
        }

        @SuppressWarnings("unchecked")
        public Predicate isNotEmpty() {
            return criteriaBuilder.notEqual((Path<String>) path, "");
        }

        @SuppressWarnings("unchecked")
        public Predicate equalsIgnoreCase(String value) {
            return criteriaBuilder.equal(
                criteriaBuilder.upper((Path<String>) path),
                value.toUpperCase()
            );
        }

        @SuppressWarnings("unchecked")
        public Predicate containsIgnoreCase(String value) {
            return criteriaBuilder.like(
                criteriaBuilder.upper((Path<String>) path),
                "%" + value.toUpperCase() + "%"
            );
        }

        @SuppressWarnings("unchecked")
        public Predicate startsWithIgnoreCase(String value) {
            return criteriaBuilder.like(
                criteriaBuilder.upper((Path<String>) path),
                value.toUpperCase() + "%"
            );
        }

        @SuppressWarnings("unchecked")
        public Predicate endsWithIgnoreCase(String value) {
            return criteriaBuilder.like(
                criteriaBuilder.upper((Path<String>) path),
                "%" + value.toUpperCase()
            );
        }

        // Number-specific methods
        public Predicate isZero() {
            return criteriaBuilder.equal(path, 0);
        }

        @SuppressWarnings("unchecked")
        public Predicate isPositive() {
            return criteriaBuilder.greaterThan((Path<Comparable>) path, (Comparable) 0);
        }

        @SuppressWarnings("unchecked")
        public Predicate isNegative() {
            return criteriaBuilder.lessThan((Path<Comparable>) path, (Comparable) 0);
        }
    }
}