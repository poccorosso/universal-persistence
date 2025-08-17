package com.example.persistence.util;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dynamic query builder with LINQ-style support using JPA CriteriaBuilder
 */
@Slf4j
public class QueryBuilder<T> {

    private final Class<T> entityClass;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;

    public QueryBuilder(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
        log.debug("Created QueryBuilder for entity: {}", entityClass.getSimpleName());
    }
    
    /**
     * Create LINQ-style query builder
     */
    public LinqQueryBuilder<T> linq() {
        log.debug("Creating LINQ-style query builder for entity: {}", entityClass.getSimpleName());
        return new LinqQueryBuilder<>(entityClass, entityManager);
    }

    public Predicate buildPredicate(List<FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            log.debug("No filters provided, returning null predicate");
            return null;
        }

        log.debug("Building predicate with {} filters", filters.size());
        CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        List<Predicate> predicates = new ArrayList<>();
        List<Predicate> orPredicates = new ArrayList<>();

        for (FilterCriteria filter : filters) {
            try {
                Predicate predicate = buildSinglePredicate(root, filter);
                if (predicate != null) {
                    if (filter.getLogicalOperator() == FilterCriteria.LogicalOperator.OR) {
                        orPredicates.add(predicate);
                        log.trace("Added OR predicate for field: {}", filter.getField());
                    } else {
                        predicates.add(predicate);
                        log.trace("Added AND predicate for field: {}", filter.getField());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to build predicate for filter: {}", filter, e);
                throw e;
            }
        }

        // Combine predicates
        Predicate finalPredicate = null;
        if (!predicates.isEmpty()) {
            finalPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }
        if (!orPredicates.isEmpty()) {
            Predicate orPredicate = criteriaBuilder.or(orPredicates.toArray(new Predicate[0]));
            finalPredicate = finalPredicate == null ? orPredicate :
                criteriaBuilder.and(finalPredicate, orPredicate);
        }

        return finalPredicate;
    }
    


    @SuppressWarnings("unchecked")
    private Predicate buildSinglePredicate(Root<T> root, FilterCriteria filter) {
        try {
            Path<?> path = getFieldPath(root, filter.getField());
            Object value = filter.getValue();

            log.trace("Building predicate: {} {} {}", filter.getField(), filter.getOperator(), value);

            switch (filter.getOperator()) {
                case EQUALS:
                    return criteriaBuilder.equal(path, value);
                case NOT_EQUALS:
                    return criteriaBuilder.notEqual(path, value);
                case GREATER_THAN:
                    return criteriaBuilder.greaterThan((Path<Comparable>) path, (Comparable) value);
                case GREATER_EQUAL:
                    return criteriaBuilder.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
                case LESS_THAN:
                    return criteriaBuilder.lessThan((Path<Comparable>) path, (Comparable) value);
                case LESS_EQUAL:
                    return criteriaBuilder.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
                case LIKE:
                    return criteriaBuilder.like((Path<String>) path, "%" + value + "%");
                case NOT_LIKE:
                    return criteriaBuilder.notLike((Path<String>) path, "%" + value + "%");
                case STARTS_WITH:
                    return criteriaBuilder.like((Path<String>) path, value.toString() + "%");
                case ENDS_WITH:
                    return criteriaBuilder.like((Path<String>) path, "%" + value.toString());
                case CONTAINS:
                    return criteriaBuilder.like((Path<String>) path, "%" + value.toString() + "%");
                case IN:
                    return path.in((Collection<?>) value);
                case NOT_IN:
                    return criteriaBuilder.not(path.in((Collection<?>) value));
                case IS_NULL:
                    return criteriaBuilder.isNull(path);
                case IS_NOT_NULL:
                    return criteriaBuilder.isNotNull(path);
                case BETWEEN:
                    if (value instanceof List && ((List<?>) value).size() == 2) {
                        List<?> values = (List<?>) value;
                        return criteriaBuilder.between((Path<Comparable>) path,
                            (Comparable) values.get(0), (Comparable) values.get(1));
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to build query condition for field: {}", filter.getField(), e);
            throw new RuntimeException("Failed to build query condition: " + filter.getField(), e);
        }

        return null;
    }

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
    
    public Pageable buildPageable(PageRequest pageRequest) {
        if (pageRequest == null) {
            log.debug("No page request provided, using default pagination");
            return org.springframework.data.domain.PageRequest.of(0, 20);
        }
        
        log.debug("Building pageable: page={}, size={}", pageRequest.getPage(), pageRequest.getSize());
        
        Sort sort = Sort.unsorted();
        if (pageRequest.getSorts() != null && !pageRequest.getSorts().isEmpty()) {
            Sort.Order[] orders = pageRequest.getSorts().stream()
                .map(s -> {
                    log.trace("Adding sort: {} {}", s.getField(), s.getDirection());
                    return s.getDirection() == PageRequest.SortCriteria.SortDirection.DESC
                        ? Sort.Order.desc(s.getField())
                        : Sort.Order.asc(s.getField());
                })
                .toArray(Sort.Order[]::new);
            sort = Sort.by(orders);
        }
        
        return org.springframework.data.domain.PageRequest.of(
            pageRequest.getPage(), 
            pageRequest.getSize(), 
            sort
        );
    }
}