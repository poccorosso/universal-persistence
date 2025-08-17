package com.example.persistence.controller;

import com.example.persistence.dto.BaseResponse;
import com.example.persistence.dto.FilterCriteria;
import com.example.persistence.dto.PageRequest;
import com.example.persistence.dto.PageResponse;
import com.example.persistence.entity.DataSourceEntity;
import com.example.persistence.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DataSource management controller
 */
@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
@Slf4j
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * Create new datasource
     */
    @PostMapping
    public ResponseEntity<BaseResponse<DataSourceEntity>> createDataSource(@RequestBody DataSourceEntity dataSource) {
        log.info("Creating datasource: {}", dataSource.getName());
        try {
            DataSourceEntity created = dataSourceService.createDataSource(dataSource);
            return ResponseEntity.ok(BaseResponse.success(created, "DataSource created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating datasource", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to create datasource"));
        }
    }

    /**
     * Update datasource
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<DataSourceEntity>> updateDataSource(@PathVariable Long id,
                                                                           @RequestBody DataSourceEntity dataSource) {
        log.info("Updating datasource ID: {}", id);
        try {
            dataSource.setId(id);
            DataSourceEntity updated = dataSourceService.updateDataSource(dataSource);
            return ResponseEntity.ok(BaseResponse.success(updated, "DataSource updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating datasource", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to update datasource"));
        }
    }

    /**
     * Get datasource by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<DataSourceEntity>> getDataSource(@PathVariable Long id) {
        log.debug("Getting datasource ID: {}", id);
        return dataSourceService.findById(id)
                .map(ds -> ResponseEntity.ok(BaseResponse.success(ds, "DataSource found")))
                .orElse(ResponseEntity.ok(BaseResponse.notFound("DataSource not found with ID: " + id)));
    }

    /**
     * Get all active datasources
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<DataSourceEntity>>> getAllActiveDataSources() {
        log.debug("Getting all active datasources");
        try {
            List<DataSourceEntity> dataSources = dataSourceService.findAllActive();
            return ResponseEntity.ok(BaseResponse.success(dataSources,
                    String.format("Found %d active datasources", dataSources.size())));
        } catch (Exception e) {
            log.error("Error getting active datasources", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to get datasources"));
        }
    }

    /**
     * Search datasources with filters
     */
    @PostMapping("/search")
    public ResponseEntity<BaseResponse<List<DataSourceEntity>>> searchDataSources(@RequestBody List<FilterCriteria> filters) {
        log.debug("Searching datasources with filters");
        try {
            List<DataSourceEntity> dataSources = dataSourceService.findDataSourcesByFilters(filters);
            return ResponseEntity.ok(BaseResponse.success(dataSources,
                    String.format("Found %d datasources matching filters", dataSources.size())));
        } catch (Exception e) {
            log.error("Error searching datasources", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to search datasources"));
        }
    }

    /**
     * Search datasources with pagination
     */
    @PostMapping("/search/page")
    public ResponseEntity<BaseResponse<PageResponse<DataSourceEntity>>> searchDataSourcesWithPage(
            @RequestBody Map<String, Object> request) {
        log.debug("Searching datasources with pagination");
        try {
            @SuppressWarnings("unchecked")
            List<FilterCriteria> filters = (List<FilterCriteria>) request.get("filters");
            PageRequest pageRequest = (PageRequest) request.get("pageRequest");

            PageResponse<DataSourceEntity> result = dataSourceService.findDataSourcesWithPage(filters, pageRequest);
            return ResponseEntity.ok(BaseResponse.success(result, "DataSources retrieved successfully"));
        } catch (Exception e) {
            log.error("Error searching datasources with pagination", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to search datasources"));
        }
    }

    /**
     * Search datasources with optional pagination
     */
    @PostMapping("/search/optional-page")
    public ResponseEntity<BaseResponse<Object>> searchDataSourcesWithOptionalPage(
            @RequestBody Map<String, Object> request) {
        log.debug("Searching datasources with optional pagination");
        try {
            @SuppressWarnings("unchecked")
            List<FilterCriteria> filters = (List<FilterCriteria>) request.get("filters");
            PageRequest pageRequest = (PageRequest) request.get("pageRequest");
            Boolean paginated = (Boolean) request.getOrDefault("paginated", false);

            Object result = dataSourceService.findDataSourcesWithOptionalPage(filters, pageRequest, paginated);
            return ResponseEntity.ok(BaseResponse.success(result, "DataSources retrieved successfully"));
        } catch (Exception e) {
            log.error("Error searching datasources with optional pagination", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to search datasources"));
        }
    }

    /**
     * Test connection to datasource
     */
    @PostMapping("/{id}/test-connection")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testConnection(@PathVariable Long id) {
        log.info("Testing connection for datasource ID: {}", id);

        try {
            boolean isConnected = dataSourceService.testConnection(id);
            Map<String, Object> connectionInfo = Map.of(
                    "dataSourceId", id,
                    "connected", isConnected
            );

            String message = isConnected ? "Connection successful" : "Connection failed";
            return ResponseEntity.ok(BaseResponse.success(connectionInfo, message));

        } catch (Exception e) {
            log.error("Error testing connection", e);
            return ResponseEntity.internalServerError().body(
                    BaseResponse.serverError("Failed to test connection"));
        }
    }

    /**
     * Execute SQL query on datasource
     */
    @PostMapping("/{id}/query")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> executeQuery(@PathVariable Long id,
                                                                                @RequestBody Map<String, Object> request) {
        log.info("Executing query on datasource ID: {}", id);

        String sql = (String) request.get("sql");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");

        try {
            List<Map<String, Object>> results = dataSourceService.executeQuery(id, sql, parameters);
            Map<String, Object> metadata = Map.of(
                    "rowCount", results.size(),
                    "dataSourceId", id,
                    "sql", sql
            );

            return ResponseEntity.ok(BaseResponse.successWithMetadata(results,
                    "Query executed successfully", metadata));

        } catch (SecurityException e) {
            log.error("Security error executing query: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(BaseResponse.badRequest("SQL query rejected: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Error executing query: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to execute query"));
        }
    }

    /**
     * Execute update SQL on datasource
     */
    @PostMapping("/{id}/update")
    public ResponseEntity<BaseResponse<Map<String, Object>>> executeUpdate(@PathVariable Long id,
                                                                           @RequestBody Map<String, Object> request) {
        log.info("Executing update on datasource ID: {}", id);

        String sql = (String) request.get("sql");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");

        try {
            int affectedRows = dataSourceService.executeUpdate(id, sql, parameters);
            Map<String, Object> updateInfo = Map.of(
                    "affectedRows", affectedRows,
                    "dataSourceId", id
            );

            return ResponseEntity.ok(BaseResponse.success(updateInfo, "Update executed successfully"));

        } catch (SecurityException e) {
            log.error("Security error executing update: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(BaseResponse.badRequest("SQL update rejected: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error executing update: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to execute update"));
        }
    }

    /**
     * Activate datasource
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<BaseResponse<Void>> activateDataSource(@PathVariable Long id) {
        log.info("Activating datasource ID: {}", id);
        try {
            dataSourceService.activateDataSource(id);
            return ResponseEntity.ok(BaseResponse.success("DataSource activated successfully"));
        } catch (Exception e) {
            log.error("Error activating datasource", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to activate datasource"));
        }
    }

    /**
     * Deactivate datasource
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<BaseResponse<Void>> deactivateDataSource(@PathVariable Long id) {
        log.info("Deactivating datasource ID: {}", id);
        try {
            dataSourceService.deactivateDataSource(id);
            return ResponseEntity.ok(BaseResponse.success("DataSource deactivated successfully"));
        } catch (Exception e) {
            log.error("Error deactivating datasource", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to deactivate datasource"));
        }
    }

    /**
     * Delete datasource
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteDataSource(@PathVariable Long id) {
        log.info("Deleting datasource ID: {}", id);
        try {
            dataSourceService.deleteDataSource(id);
            return ResponseEntity.ok(BaseResponse.success("DataSource deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting datasource", e);
            return ResponseEntity.internalServerError().body(BaseResponse.serverError("Failed to delete datasource"));
        }
    }
}
