package com.example.persistence.repository;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import com.example.persistence.dto.PageResponse;
import com.example.persistence.util.LinqQueryBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base Repository interface providing basic CRUD and dynamic query functionality
 */
@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable>
    extends JpaRepository<T, ID> {
    
    /**
     * Find entities by filter criteria
     */
    List<T> findByFilters(List<FilterCriteria> filters);
    
    /**
     * Find entities by filter criteria with pagination
     */
    PageResponse<T> findByFiltersWithPage(List<FilterCriteria> filters, PageRequest pageRequest);
    
    /**
     * Find entities by filter criteria with optional pagination
     */
    Object findByFiltersWithOptionalPage(List<FilterCriteria> filters, PageRequest pageRequest, boolean paginated);
    
    /**
     * Execute native SQL query
     */
    List<Map<String, Object>> executeNativeQuery(String sql, Map<String, Object> parameters);
    
    /**
     * Execute native SQL query and map to entity
     */
    List<T> executeNativeQueryForEntity(String sql, Map<String, Object> parameters);
    
    /**
     * Batch insert entities
     */
    List<T> batchInsert(List<T> entities);
    
    /**
     * Batch update entities
     */
    List<T> batchUpdate(List<T> entities);
    
    /**
     * Batch delete entities by IDs
     */
    void batchDeleteByIds(List<ID> ids);
    
    /**
     * Create or update single entity
     */
    T createOrUpdate(T entity);
    
    /**
     * Create or update list of entities
     */
    List<T> createOrUpdateBatch(List<T> entities);
    
    /**
     * Soft delete by ID (if entity supports soft delete)
     */
    void softDeleteById(ID id);
    
    /**
     * Find active entities (if entity supports soft delete)
     */
    List<T> findActiveEntities();
    
    /**
     * Get LINQ-style query builder
     */
    LinqQueryBuilder<T> linq();
}