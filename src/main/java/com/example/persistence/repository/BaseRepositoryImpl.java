package com.example.persistence.repository;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import com.example.persistence.dto.PageResponse;
import com.example.persistence.util.LinqQueryBuilder;
import com.example.persistence.util.QueryBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base Repository implementation class
 */
@Slf4j
public class BaseRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final QueryBuilder<T> queryBuilder;
    private final Class<T> entityClass;
    private final JpaEntityInformation<T, ID> entityInformation;

    public BaseRepositoryImpl(JpaEntityInformation<T, ID> entityInformation,
                              EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
        this.entityClass = entityInformation.getJavaType();
        this.entityInformation = entityInformation;
        this.queryBuilder = new QueryBuilder<>(entityClass, entityManager);

        log.debug("Initialized BaseRepository for entity: {}", entityClass.getSimpleName());
    }

    @Override
    public List<T> findByFilters(List<FilterCriteria> filters) {
        log.debug("Finding entities by filters: {}", filters);
        try {
            Predicate predicate = queryBuilder.buildPredicate(filters);
            if (predicate == null) {
                return findAll();
            }

            CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            query.select(root).where(predicate);

            TypedQuery<T> typedQuery = entityManager.createQuery(query);
            List<T> results = typedQuery.getResultList();
            log.debug("Found {} entities matching filters", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error finding entities by filters: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public PageResponse<T> findByFiltersWithPage(List<FilterCriteria> filters,
                                                 PageRequest pageRequest) {
        log.debug("Finding entities by filters with pagination: filters={}, pageRequest={}", filters, pageRequest);
        try {
            Predicate predicate = queryBuilder.buildPredicate(filters);
            Pageable pageable = queryBuilder.buildPageable(pageRequest);

            // Get total count
            CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
            Root<T> countRoot = countQuery.from(entityClass);
            countQuery.select(criteriaBuilder.count(countRoot));
            if (predicate != null) {
                countQuery.where(predicate);
            }
            long totalElements = entityManager.createQuery(countQuery).getSingleResult();

            // Get page content
            CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            query.select(root);
            if (predicate != null) {
                query.where(predicate);
            }

            // Apply sorting
            if (pageable.getSort().isSorted()) {
                query.orderBy(pageable.getSort().stream()
                        .map(order -> order.isAscending() ?
                                criteriaBuilder.asc(root.get(order.getProperty())) :
                                criteriaBuilder.desc(root.get(order.getProperty())))
                        .toArray(jakarta.persistence.criteria.Order[]::new));
            }

            TypedQuery<T> typedQuery = entityManager.createQuery(query);
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());

            List<T> content = typedQuery.getResultList();
            Page<T> page = new PageImpl<>(content, pageable, totalElements);

            PageResponse<T> response = PageResponse.<T>builder()
                    .content(page.getContent())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .currentPage(page.getNumber())
                    .pageSize(page.getSize())
                    .build();

            log.debug("Found {} entities in page {} of {}", page.getContent().size(),
                    page.getNumber(), page.getTotalPages());
            return response;
        } catch (Exception e) {
            log.error("Error finding entities by filters with pagination: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Object findByFiltersWithOptionalPage(List<FilterCriteria> filters,
                                                PageRequest pageRequest, boolean paginated) {
        log.debug("Finding entities with optional pagination: paginated={}", paginated);
        if (paginated && pageRequest != null) {
            return findByFiltersWithPage(filters, pageRequest);
        } else {
            return findByFilters(filters);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> executeNativeQuery(String sql,
                                                        Map<String, Object> parameters) {
        log.debug("Executing native query: {}", sql);
        try {
            Query query = entityManager.createNativeQuery(sql);
            if (parameters != null) {
                parameters.forEach((key, value) -> {
                    log.trace("Setting parameter: {} = {}", key, value);
                    query.setParameter(key, value);
                });
            }
            List<Map<String, Object>> results = query.getResultList();
            log.debug("Native query returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error executing native query: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> executeNativeQueryForEntity(String sql,
                                               Map<String, Object> parameters) {
        log.debug("Executing native query for entity: {}", sql);
        try {
            Query query = entityManager.createNativeQuery(sql, entityClass);
            if (parameters != null) {
                parameters.forEach((key, value) -> {
                    log.trace("Setting parameter: {} = {}", key, value);
                    query.setParameter(key, value);
                });
            }
            List<T> results = query.getResultList();
            log.debug("Native query for entity returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error executing native query for entity: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public List<T> batchInsert(List<T> entities) {
        log.debug("Batch inserting {} entities", entities.size());
        try {
            int batchSize = 50;
            for (int i = 0; i < entities.size(); i++) {
                entityManager.persist(entities.get(i));
                if (i % batchSize == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                    log.trace("Flushed batch at index {}", i);
                }
            }
            entityManager.flush();
            entityManager.clear();
            log.debug("Successfully batch inserted {} entities", entities.size());
            return entities;
        } catch (Exception e) {
            log.error("Error batch inserting entities: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public List<T> batchUpdate(List<T> entities) {
        log.debug("Batch updating {} entities", entities.size());
        try {
            int batchSize = 50;
            for (int i = 0; i < entities.size(); i++) {
                entityManager.merge(entities.get(i));
                if (i % batchSize == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                    log.trace("Flushed batch at index {}", i);
                }
            }
            entityManager.flush();
            entityManager.clear();
            log.debug("Successfully batch updated {} entities", entities.size());
            return entities;
        } catch (Exception e) {
            log.error("Error batch updating entities: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void batchDeleteByIds(List<ID> ids) {
        log.debug("Batch deleting {} entities by IDs", ids.size());
        try {
            deleteAllById(ids);
            log.debug("Successfully batch deleted {} entities", ids.size());
        } catch (Exception e) {
            log.error("Error batch deleting entities: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public T createOrUpdate(T entity) {
        log.debug("Creating or updating entity: {}", entity.getClass().getSimpleName());
        try {
            ID id = entityInformation.getId(entity);
            if (id == null || !existsById(id)) {
                log.debug("Creating new entity");
                return save(entity);
            } else {
                log.debug("Updating existing entity with ID: {}", id);
                return save(entity);
            }
        } catch (Exception e) {
            log.error("Error creating or updating entity: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public List<T> createOrUpdateBatch(List<T> entities) {
        log.debug("Batch creating or updating {} entities", entities.size());
        try {
            return saveAll(entities);
        } catch (Exception e) {
            log.error("Error batch creating or updating entities: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void softDeleteById(ID id) {
        log.debug("Soft deleting entity with ID: {}", id);
        try {
            Optional<T> entityOpt = findById(id);
            if (entityOpt.isPresent()) {
                T entity = entityOpt.get();
                // Try to set deleted flag if entity supports soft delete
                try {
                    Field deletedField = entityClass.getDeclaredField("deleted");
                    deletedField.setAccessible(true);
                    deletedField.set(entity, true);
                    save(entity);
                    log.debug("Successfully soft deleted entity with ID: {}", id);
                } catch (NoSuchFieldException e) {
                    log.warn("Entity {} does not support soft delete, performing hard delete", entityClass.getSimpleName());
                    deleteById(id);
                }
            } else {
                log.warn("Entity with ID {} not found for soft delete", id);
            }
        } catch (Exception e) {
            log.error("Error soft deleting entity: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<T> findActiveEntities() {
        log.debug("Finding active entities");
        try {
            // Try to find entities where deleted = false
            try {
                entityClass.getDeclaredField("deleted");
                List<FilterCriteria> filters = List.of(
                        FilterCriteria.builder()
                                .field("deleted")
                                .operator(FilterCriteria.FilterOperator.EQUALS)
                                .value(false)
                                .build()
                );
                return findByFilters(filters);
            } catch (NoSuchFieldException e) {
                log.debug("Entity {} does not support soft delete, returning all entities", entityClass.getSimpleName());
                return findAll();
            }
        } catch (Exception e) {
            log.error("Error finding active entities: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get LINQ-style query builder
     */
    public LinqQueryBuilder<T> linq() {
        log.debug("Creating LINQ query builder for entity: {}", entityClass.getSimpleName());
        return queryBuilder.linq();
    }
}