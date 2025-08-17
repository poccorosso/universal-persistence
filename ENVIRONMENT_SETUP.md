# Environment Configuration Guide

This guide explains how to use the different environment profiles available in the Universal Persistence Framework.

## Available Environment Profiles

| Profile | Purpose | Database | Configuration File |
|---------|---------|----------|-------------------|
| `h2` | Development/Testing | In-memory H2 | `application-h2.yml` |
| `postgresql` | Production | PostgreSQL | `application-postgresql.yml` |
| `mysql` | Production | MySQL | `application-mysql.yml` |
| `oracle` | Enterprise | Oracle | `application-oracle.yml` |
| `prod` | Production | PostgreSQL (optimized) | `application-prod.yml` |
| `dev` | Development | H2 (default) | `application-dev.yml` |

## How to Use Different Environments

### 1. Command Line Arguments
```bash
# Run with specific profile
java -jar target/persistence-framework.jar --spring.profiles.active=postgresql

# Run with multiple profiles
java -jar target/persistence-framework.jar --spring.profiles.active=prod,postgresql
```

### 2. Environment Variables
```bash
# Set profile via environment variable
export SPRING_PROFILES_ACTIVE=mysql
java -jar target/persistence-framework.jar

# Windows
set SPRING_PROFILES_ACTIVE=oracle
java -jar target/persistence-framework.jar
```

### 3. Application Properties
```bash
# In application.yml
spring:
  profiles:
    active: h2
```

### 4. Maven Commands
```bash
# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=h2

# Run tests with specific profile
mvn test -Dspring.profiles.active=postgresql
```

## Database Setup Instructions

### H2 (Development/Testing)
```bash
# No setup required - runs automatically
# Access H2 Console: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# Username: sa
# Password: (empty)
```

### PostgreSQL
```bash
# Using Docker
docker run -d --name postgres-db \
  -e POSTGRES_DB=your_database \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 postgres:13

# Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=password
```

### MySQL
```bash
# Using Docker
docker run -d --name mysql-db \
  -e MYSQL_DATABASE=your_database \
  -e MYSQL_ROOT_PASSWORD=password \
  -p 3306:3306 mysql:8

# Set environment variables
export DB_USERNAME=root
export DB_PASSWORD=password
```

### Oracle
```bash
# Using Docker
docker run -d --name oracle-db \
  -e ORACLE_PASSWORD=password \
  -p 1521:1521 gvenzl/oracle-xe:21-slim

# Set environment variables
export DB_USERNAME=system
export DB_PASSWORD=password
```

## Environment-Specific Features

### H2 Profile Features
- ✅ In-memory database (no persistence)
- ✅ H2 Console enabled
- ✅ SQL logging enabled
- ✅ Fast startup/shutdown
- ✅ Ideal for unit testing

### Production Profiles (PostgreSQL, MySQL, Oracle)
- ✅ Connection pooling optimized
- ✅ SQL logging disabled
- ✅ Production-grade settings
- ✅ Environment variable support
- ✅ SSL/TLS ready

### Prod Profile Features
- ✅ Maximum performance tuning
- ✅ Production security settings
- ✅ Monitoring endpoints
- ✅ Log rotation
- ✅ Connection leak detection
- ✅ Production cache settings

## Environment Variables Reference

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `your_database` |
| `DB_USERNAME` | Database username | Varies by profile |
| `DB_PASSWORD` | Database password | Varies by profile |

## Quick Start Examples

### Development with H2
```bash
# Simple development setup
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Production with PostgreSQL
```bash
# Production setup with environment variables
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
java -jar target/persistence-framework.jar --spring.profiles.active=prod
```

### Testing with MySQL
```bash
# Testing with MySQL
docker run -d --name test-mysql -e MYSQL_DATABASE=test_db -e MYSQL_ROOT_PASSWORD=test -p 3306:3306 mysql:8
export DB_USERNAME=root
export DB_PASSWORD
