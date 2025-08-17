package com.example.persistence.config;

import com.example.persistence.repository.BaseRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Repository配置类
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.persistence.repository",
    repositoryBaseClass = BaseRepositoryImpl.class
)
public class RepositoryConfig {
}