package com.example.persistence.service;

import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import com.example.persistence.dto.PageResponse;
import com.example.persistence.entity.DataSourceEntity;
import com.example.persistence.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DataSource management service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DataSourceService {
    
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceConnectionService connectionService;
    
    /**
     * Create new datasource
     */
    @Transactional
    @CacheEvict(value = "datasources", allEntries = true)
    public DataSourceEntity createDataSource(DataSourceEntity dataSource) {
        log.info("Creating new datasource: {}", dataSource.getName());
        
        // Check if name already exists
        if (dataSourceRepository.existsByNameAndDeletedFalse(dataSource.getName())) {
            throw new IllegalArgumentException("DataSource with name '" + dataSource.getName() + "' already exists");
        }
        
        // Set default values
        if (dataSource.getActive() == null) {
            dataSource.setActive(true);
        }
        if (dataSource.getDeleted() == null) {
            dataSource.setDeleted(false);
        }
        
        DataSourceEntity saved = dataSourceRepository.save(dataSource);
        log.info("Successfully created datasource with ID: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Update existing datasource
     */
    @Transactional
    public DataSourceEntity updateDataSource(DataSourceEntity dataSource) {
        log.info("Updating datasource: {}", dataSource.getName());
        
        Optional<DataSourceEntity> existing = dataSourceRepository.findById(dataSource.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("DataSource with ID " + dataSource.getId() + " not found");
        }
        
        // Close existing connection pool if connection details changed
        DataSourceEntity existingDs = existing.get();
        if (connectionDetailsChanged(existingDs, dataSource)) {
            log.info("Connection details changed, closing existing connection pool");
            connectionService.closeConnectionPool(dataSource.getId());
        }
        
        DataSourceEntity updated = dataSourceRepository.save(dataSource);
        log.info("Successfully updated datasource with ID: {}", updated.getId());
        
        return updated;
    }
    
    /**
     * Find datasource by ID
     */
    @Cacheable(value = "datasources", key = "#id")
    public Optional<DataSourceEntity> findById(Long id) {
        log.debug("Finding datasource by ID: {}", id);
        return dataSourceRepository.findById(id)
                .filter(ds -> !ds.getDeleted());
    }
    
    /**
     * Find datasource by name
     */
    @Cacheable(value = "datasources", key = "'name:' + #name")
    public Optional<DataSourceEntity> findByName(String name) {
        log.debug("Finding datasource by name: {}", name);
        return dataSourceRepository.findByNameAndDeletedFalse(name);
    }
    
    /**
     * Find all active datasources
     */
    @Cacheable(value = "datasources", key = "'active-all'")
    public List<DataSourceEntity> findAllActive() {
        log.debug("Finding all active datasources");
        return dataSourceRepository.findByActiveTrueAndDeletedFalse();
    }
    
    /**
     * Find datasources with filters
     */
    public List<DataSourceEntity> findDataSourcesByFilters(List<FilterCriteria> filters) {
        log.debug("Finding datasources by filters: {}", filters);
        return dataSourceRepository.findByFilters(filters);
    }
    
    /**
     * Find datasources with pagination
     */
    public PageResponse<DataSourceEntity> findDataSourcesWithPage(List<FilterCriteria> filters, 
                                                                 PageRequest pageRequest) {
        log.debug("Finding datasources with pagination");
        return dataSourceRepository.findByFiltersWithPage(filters, pageRequest);
    }
    
    /**
     * Find datasources with optional pagination
     */
    public Object findDataSourcesWithOptionalPage(List<FilterCriteria> filters, 
                                                 PageRequest pageRequest, 
                                                 boolean paginated) {
        log.debug("Finding datasources with optional pagination: {}", paginated);
        return dataSourceRepository.findByFiltersWithOptionalPage(filters, pageRequest, paginated);
    }
    
    /**
     * Test connection to datasource
     */
    public boolean testConnection(Long dataSourceId) {
        log.info("Testing connection for datasource ID: {}", dataSourceId);
        
        Optional<DataSourceEntity> dataSourceOpt = findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            log.error("DataSource with ID {} not found", dataSourceId);
            return false;
        }
        
        return connectionService.testConnection(dataSourceOpt.get());
    }
    
    /**
     * Execute SQL query on datasource
     */
    @Cacheable(value = "query-results", key = "#dataSourceId + ':' + #sql.hashCode() + ':' + (#parameters != null ? #parameters.hashCode() : 0)")
    public List<Map<String, Object>> executeQuery(Long dataSourceId, 
                                                  String sql, 
                                                  Map<String, Object> parameters) {
        log.info("Executing query on datasource ID: {}", dataSourceId);
        
        Optional<DataSourceEntity> dataSourceOpt = findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            throw new IllegalArgumentException("DataSource with ID " + dataSourceId + " not found");
        }
        
        DataSourceEntity dataSource = dataSourceOpt.get();
        if (!dataSource.getActive()) {
            throw new IllegalStateException("DataSource " + dataSource.getName() + " is not active");
        }
        
        return connectionService.executeQuery(dataSource, sql, parameters);
    }
    
    /**
     * Execute update SQL on datasource
     */
    @Transactional
    public int executeUpdate(Long dataSourceId, 
                            String sql, 
                            Map<String, Object> parameters) {
        log.info("Executing update on datasource ID: {}", dataSourceId);
        
        Optional<DataSourceEntity> dataSourceOpt = findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            throw new IllegalArgumentException("DataSource with ID " + dataSourceId + " not found");
        }
        
        DataSourceEntity dataSource = dataSourceOpt.get();
        if (!dataSource.getActive()) {
            throw new IllegalStateException("DataSource " + dataSource.getName() + " is not active");
        }
        
        return connectionService.executeUpdate(dataSource, sql, parameters);
    }
    
    /**
     * Activate datasource
     */
    @Transactional
    public void activateDataSource(Long dataSourceId) {
        log.info("Activating datasource ID: {}", dataSourceId);
        
        Optional<DataSourceEntity> dataSourceOpt = dataSourceRepository.findById(dataSourceId);
        if (dataSourceOpt.isPresent()) {
            DataSourceEntity dataSource = dataSourceOpt.get();
            dataSource.setActive(true);
            dataSourceRepository.save(dataSource);
            log.info("Successfully activated datasource: {}", dataSource.getName());
        }
    }
    
    /**
     * Deactivate datasource
     */
    @Transactional
    public void deactivateDataSource(Long dataSourceId) {
        log.info("Deactivating datasource ID: {}", dataSourceId);
        
        Optional<DataSourceEntity> dataSourceOpt = dataSourceRepository.findById(dataSourceId);
        if (dataSourceOpt.isPresent()) {
            DataSourceEntity dataSource = dataSourceOpt.get();
            dataSource.setActive(false);
            dataSourceRepository.save(dataSource);
            
            // Close connection pool
            connectionService.closeConnectionPool(dataSourceId);
            
            log.info("Successfully deactivated datasource: {}", dataSource.getName());
        }
    }
    
    /**
     * Soft delete datasource
     */
    @Transactional
    public void deleteDataSource(Long dataSourceId) {
        log.info("Soft deleting datasource ID: {}", dataSourceId);
        
        Optional<DataSourceEntity> dataSourceOpt = dataSourceRepository.findById(dataSourceId);
        if (dataSourceOpt.isPresent()) {
            DataSourceEntity dataSource = dataSourceOpt.get();
            dataSource.setDeleted(true);
            dataSource.setActive(false);
            dataSourceRepository.save(dataSource);
            
            // Close connection pool
            connectionService.closeConnectionPool(dataSourceId);
            
            log.info("Successfully deleted datasource: {}", dataSource.getName());
        }
    }
    
    /**
     * Check if connection details changed
     */
    private boolean connectionDetailsChanged(DataSourceEntity existing, DataSourceEntity updated) {
        return !existing.getHost().equals(updated.getHost()) ||
               !existing.getPort().equals(updated.getPort()) ||
               !existing.getDatabaseName().equals(updated.getDatabaseName()) ||
               !existing.getUid().equals(updated.getUid()) ||
               !existing.getPassword().equals(updated.getPassword()) ||
               !existing.getDatabaseType().equals(updated.getDatabaseType());
    }
}