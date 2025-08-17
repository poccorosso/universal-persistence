package com.example.persistence.repository;

import com.example.persistence.entity.DataSourceEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DataSource Repository
 */
@Repository
public interface DataSourceRepository extends BaseRepository<DataSourceEntity, Long> {
    
    /**
     * Find datasource by name
     */
    Optional<DataSourceEntity> findByNameAndDeletedFalse(String name);
    
    /**
     * Find all active datasources
     */
    List<DataSourceEntity> findByActiveTrueAndDeletedFalse();
    
    /**
     * Find datasources by database type
     */
    List<DataSourceEntity> findByDatabaseTypeAndActiveTrueAndDeletedFalse(
        DataSourceEntity.DatabaseType databaseType);
    
    /**
     * Check if datasource name exists
     */
    boolean existsByNameAndDeletedFalse(String name);
    
    /**
     * Find datasources by host
     */
    @Query("SELECT d FROM DataSourceEntity d WHERE d.host = :host AND d.active = true AND d.deleted = false")
    List<DataSourceEntity> findByHost(@Param("host") String host);
}