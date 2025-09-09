package com.example.persistence.service;

import com.example.persistence.entity.DataSourceEntity;
import com.example.persistence.util.SqlInjectionValidator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing database connections and executing queries
 */
@Service
@Slf4j
public class DataSourceConnectionService {
    
    private final Map<Long, HikariDataSource> connectionPools = new ConcurrentHashMap<>();
    private final SqlInjectionValidator sqlValidator;
    
    public DataSourceConnectionService() {
        this.sqlValidator = new SqlInjectionValidator();
    }
    
    /**
     * Get or create connection pool for datasource
     */
    public DataSource getConnectionPool(DataSourceEntity dataSourceEntity) {
        log.debug("Getting connection pool for datasource: {}", dataSourceEntity.getName());
        
        return connectionPools.computeIfAbsent(dataSourceEntity.getId(), id -> {
            log.info("Creating new connection pool for datasource: {}", dataSourceEntity.getName());
            return createConnectionPool(dataSourceEntity);
        });
    }
    
    /**
     * Create HikariCP connection pool
     */
    private HikariDataSource createConnectionPool(DataSourceEntity dataSourceEntity) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dataSourceEntity.buildConnectionUrl());
            config.setUsername(dataSourceEntity.getUid());
            config.setPassword(dataSourceEntity.getPassword());
            config.setDriverClassName(dataSourceEntity.getDriverClassName());
            
            // Pool configuration
            config.setMaximumPoolSize(dataSourceEntity.getMaxPoolSize());
            config.setMinimumIdle(dataSourceEntity.getMinPoolSize());
            config.setConnectionTimeout(dataSourceEntity.getConnectionTimeout());
            config.setIdleTimeout(dataSourceEntity.getIdleTimeout());
            config.setMaxLifetime(dataSourceEntity.getMaxLifetime());
            
            // Connection validation
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);
            
            // Pool name
            config.setPoolName("HikariPool-" + dataSourceEntity.getName());
            
            // Additional properties based on database type
            configureAdditionalProperties(config, dataSourceEntity);
            
            HikariDataSource dataSource = new HikariDataSource(config);
            log.info("Successfully created connection pool for datasource: {}", dataSourceEntity.getName());
            return dataSource;
            
        } catch (Exception e) {
            log.error("Failed to create connection pool for datasource: {}", dataSourceEntity.getName(), e);
            throw new RuntimeException("Failed to create connection pool", e);
        }
    }
    
    /**
     * Configure additional properties based on database type
     */
    private void configureAdditionalProperties(HikariConfig config, DataSourceEntity dataSourceEntity) {
        switch (dataSourceEntity.getDatabaseType()) {
            case SQLSERVER:
                config.addDataSourceProperty("encrypt", "true");
                config.addDataSourceProperty("trustServerCertificate", "true");
                config.addDataSourceProperty("loginTimeout", "30");
                config.addDataSourceProperty("queryTimeout", "30");
                break;
            case POSTGRESQL:
                config.addDataSourceProperty("stringtype", "unspecified");
                break;
            case ORACLE:
                config.addDataSourceProperty("oracle.jdbc.ReadTimeout", "30000");
                break;
            case DENODO:
                config.addDataSourceProperty("queryTimeout", dataSourceEntity.getQueryTimeout());
                break;
            default:
                break;
        }
    }
    
    /**
     * Execute SQL query with timeout and injection protection
     */
    public List<Map<String, Object>> executeQuery(DataSourceEntity dataSourceEntity, 
                                                  String sql, 
                                                  Map<String, Object> parameters) {
        log.debug("Executing query on datasource: {}", dataSourceEntity.getName());
        
        // Validate SQL for injection
        if (!sqlValidator.isValidSql(sql)) {
            log.error("SQL injection detected in query: {}", sql);
            throw new SecurityException("Potential SQL injection detected");
        }
        
        DataSource dataSource = getConnectionPool(dataSourceEntity);
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            // Set query timeout
            statement.setQueryTimeout(dataSourceEntity.getQueryTimeout());
            
            // Set parameters
            if (parameters != null) {
                setParameters(statement, parameters);
            }
            
            log.debug("Executing SQL: {}", sql);
            long startTime = System.currentTimeMillis();
            
            try (ResultSet resultSet = statement.executeQuery()) {
                results = convertResultSetToList(resultSet);
                
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Query executed successfully in {}ms, returned {} rows", 
                         executionTime, results.size());
            }
            
        } catch (SQLException e) {
            log.error("Error executing query on datasource {}: {}", dataSourceEntity.getName(), e.getMessage(), e);
            throw new RuntimeException("Query execution failed", e);
        }
        
        return results;
    }
    
    /**
     * Execute update/insert/delete SQL
     */
    public int executeUpdate(DataSourceEntity dataSourceEntity, 
                            String sql, 
                            Map<String, Object> parameters) {
        log.debug("Executing update on datasource: {}", dataSourceEntity.getName());
        
        // Validate SQL for injection
        if (!sqlValidator.isValidSql(sql)) {
            log.error("SQL injection detected in update: {}", sql);
            throw new SecurityException("Potential SQL injection detected");
        }
        
        DataSource dataSource = getConnectionPool(dataSourceEntity);
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            // Set query timeout
            statement.setQueryTimeout(dataSourceEntity.getQueryTimeout());
            
            // Set parameters
            if (parameters != null) {
                setParameters(statement, parameters);
            }
            
            log.debug("Executing update SQL: {}", sql);
            long startTime = System.currentTimeMillis();
            
            int affectedRows = statement.executeUpdate();
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("Update executed successfully in {}ms, affected {} rows", 
                     executionTime, affectedRows);
            
            return affectedRows;
            
        } catch (SQLException e) {
            log.error("Error executing update on datasource {}: {}", dataSourceEntity.getName(), e.getMessage(), e);
            throw new RuntimeException("Update execution failed", e);
        }
    }
    
    /**
     * Test connection to datasource
     */
    public boolean testConnection(DataSourceEntity dataSourceEntity) {
        log.debug("Testing connection to datasource: {}", dataSourceEntity.getName());
        
        try {
            DataSource dataSource = getConnectionPool(dataSourceEntity);
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 seconds timeout
                log.debug("Connection test for datasource {}: {}", 
                         dataSourceEntity.getName(), isValid ? "SUCCESS" : "FAILED");
                return isValid;
            }
        } catch (Exception e) {
            log.error("Connection test failed for datasource {}: {}", 
                     dataSourceEntity.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Close connection pool for datasource
     */
    public void closeConnectionPool(Long dataSourceId) {
        HikariDataSource dataSource = connectionPools.remove(dataSourceId);
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Closing connection pool for datasource ID: {}", dataSourceId);
            dataSource.close();
        }
    }
    
    /**
     * Close all connection pools
     */
    public void closeAllConnectionPools() {
        log.info("Closing all connection pools");
        connectionPools.values().forEach(dataSource -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        });
        connectionPools.clear();
    }
    
    /**
     * Set parameters in prepared statement
     */
    private void setParameters(PreparedStatement statement, Map<String, Object> parameters) throws SQLException {
        int index = 1;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();
            log.trace("Setting parameter {}: {} = {}", index, entry.getKey(), value);
            
            if (value == null) {
                statement.setNull(index, Types.NULL);
            } else if (value instanceof String) {
                statement.setString(index, (String) value);
            } else if (value instanceof Integer) {
                statement.setInt(index, (Integer) value);
            } else if (value instanceof Long) {
                statement.setLong(index, (Long) value);
            } else if (value instanceof Double) {
                statement.setDouble(index, (Double) value);
            } else if (value instanceof Boolean) {
                statement.setBoolean(index, (Boolean) value);
            } else if (value instanceof java.util.Date) {
                statement.setDate(index, Date.valueOf((LocalDate) value));
            } else if (value instanceof Timestamp) {
                statement.setTimestamp(index, (Timestamp) value);
            } else {
                statement.setObject(index, value);
            }
            index++;
        }
    }
    
    /**
     * Convert ResultSet to List of Maps
     */
    private List<Map<String, Object>> convertResultSetToList(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }
        
        return results;
    }
}
