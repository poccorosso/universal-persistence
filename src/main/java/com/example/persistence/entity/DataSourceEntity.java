package com.example.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DataSource configuration entity
 */
@Entity
@Table(name = "data_sources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatabaseType databaseType;
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(nullable = false)
    private String databaseName;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "connection_url")
    private String connectionUrl;
    
    @Column(name = "driver_class_name")
    private String driverClassName;
    
    @Column(name = "max_pool_size")
    private Integer maxPoolSize = 10;
    
    @Column(name = "min_pool_size")
    private Integer minPoolSize = 1;
    
    @Column(name = "connection_timeout")
    private Integer connectionTimeout = 30000; // 30 seconds
    
    @Column(name = "idle_timeout")
    private Integer idleTimeout = 600000; // 10 minutes
    
    @Column(name = "max_lifetime")
    private Integer maxLifetime = 1800000; // 30 minutes
    
    @Column(name = "query_timeout")
    private Integer queryTimeout = 30; // 30 seconds
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum DatabaseType {
        SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://{host}:{port};databaseName={database}"),
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://{host}:{port}/{database}"),
        ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@{host}:{port}:{database}"),
        H2("org.h2.Driver", "jdbc:h2:mem:{database}"),
        DENODO("com.denodo.vdp.jdbc.Driver", "jdbc:vdb://{host}:{port}/{database}");
        
        private final String driverClassName;
        private final String urlTemplate;
        
        DatabaseType(String driverClassName, String urlTemplate) {
            this.driverClassName = driverClassName;
            this.urlTemplate = urlTemplate;
        }
        
        public String getDriverClassName() {
            return driverClassName;
        }
        
        public String buildUrl(String host, Integer port, String database) {
            return urlTemplate
                .replace("{host}", host)
                .replace("{port}", port.toString())
                .replace("{database}", database);
        }
    }
    
    /**
     * Build connection URL based on database type
     */
    public String buildConnectionUrl() {
        if (connectionUrl != null && !connectionUrl.trim().isEmpty()) {
            return connectionUrl;
        }
        return databaseType.buildUrl(host, port, databaseName);
    }
    
    /**
     * Get driver class name based on database type
     */
    public String getDriverClassName() {
        if (driverClassName != null && !driverClassName.trim().isEmpty()) {
            return driverClassName;
        }
        return databaseType.getDriverClassName();
    }
}
