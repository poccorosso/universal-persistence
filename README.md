# Universal Persistence Framework

A comprehensive persistence framework based on Spring Boot and Spring Data JPA, supporting multiple databases, dynamic queries, and flexible CRUD operations with LINQ-style query builder.

## Features

- 🚀 **Multi-Database Support**: MySQL, PostgreSQL, Oracle, H2, Denodo
- 🔍 **Dynamic Queries**: Flexible filtering with QueryDSL
- 📄 **Pagination Support**: Built-in pagination and sorting
- 💾 **Batch Operations**: Efficient batch insert and update
- 🛠️ **Native SQL Support**: Execute custom SQL queries
- 🎯 **Zero SQL Writing**: Most operations require no manual SQL
- 🔗 **LINQ-Style Queries**: C#-like fluent query API
- 🗂️ **Field Projections**: Select specific fields with type safety
- 🚀 **Caching Layer**: Built-in caching with Caffeine
- 🔒 **SQL Injection Protection**: Built-in security validation
- 🌐 **DataSource Management**: Dynamic database connection management

## Quick Start

### 1. Create Entity Class

```java
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String email;
    
    private String fullName;
    private Integer age;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
```

### 2. Create Repository Interface

```java
@Repository
public interface UserRepository extends BaseRepository<User, Long> {
    // Inherit BaseRepository to get all common functionality
    // Can add custom query methods
    Optional<User> findByUsername(String username);
}
```

### 3. Using Dynamic Queries

```java
// Create filter criteria
List<FilterCriteria> filters = Arrays.asList(
    FilterCriteria.builder()
        .field("status")
        .operator(FilterCriteria.FilterOperator.EQUALS)
        .value(User.Status.ACTIVE)
        .build(),
    FilterCriteria.builder()
        .field("age")
        .operator(FilterCriteria.FilterOperator.GREATER_THAN)
        .value(25)
        .logicalOperator(FilterCriteria.LogicalOperator.AND)
        .build()
);

// Execute query
List<User> users = userRepository.findByFilters(filters);
```

### 4. Pagination Query

```java
// Create pagination request
PageRequest pageRequest = PageRequest.builder()
    .page(0)
    .size(10)
    .sorts(Arrays.asList(
        PageRequest.SortCriteria.builder()
            .field("createdAt")
            .direction(PageRequest.SortCriteria.SortDirection.DESC)
            .build()
    ))
    .build();

// Execute pagination query
PageResponse<User> result = userRepository.findByFiltersWithPage(filters, pageRequest);
```

### 5. Execute Native SQL

```java
// Execute native SQL query
String sql = "SELECT * FROM users WHERE age > :minAge ORDER BY created_at DESC";
Map<String, Object> parameters = Map.of("minAge", 25);

List<Map<String, Object>> result = userRepository.executeNativeQuery(sql, parameters);

// Or map to entity
List<User> users = userRepository.executeNativeQueryForEntity(sql, parameters);
```

### 6. Batch Operations

```java
// Batch insert
List<User> users = Arrays.asList(user1, user2, user3);
List<User> inserted = userRepository.batchInsert(users);

// Batch update
List<User> updated = userRepository.batchUpdate(users);
```

## LINQ-Style Query Examples

The framework provides a C#-like LINQ-style query builder for more intuitive and readable queries.

### 1. Basic Filtering (Where)

Find all active users older than 25.

```java
List<User> users = linqQueryBuilderFactory.createQuery(User.class)
    .where(user -> user.getStatus() == User.Status.ACTIVE && user.getAge() > 25)
    .toList();
```

### 2. Complex Conditions

Find active users who are older than 30 or have "admin" in their username.

```java
List<User> users = linqQueryBuilderFactory.createQuery(User.class)
    .where(user -> user.getStatus() == User.Status.ACTIVE && (user.getAge() > 30 || user.getUsername().contains("admin")))
    .toList();
```

### 3. Projections (Select)

Select only the `username` and `email` of all users.

```java
List<Map<String, Object>> users = linqQueryBuilderFactory.createQuery(User.class)
    .select("username", "email")
    .toList();
```

Or project into a DTO:

```java
List<UserProjection> users = linqQueryBuilderFactory.createQuery(User.class)
    .select(UserProjection.class)
    .toList();
```

### 4. Ordering (OrderBy)

Get the top 5 youngest users.

```java
List<User> users = linqQueryBuilderFactory.createQuery(User.class)
    .orderBy(User::getAge)
    .take(5)
    .toList();
```

### 5. Pagination (Skip/Take)

Get the second page of 10 active users.

```java
Page<User> userPage = linqQueryBuilderFactory.createQuery(User.class)
    .where(user -> user.getStatus() == User.Status.ACTIVE)
    .toPage(1, 10); // page index, page size
```

### 6. Aggregates

Count the number of active users.

```java
long count = linqQueryBuilderFactory.createQuery(User.class)
    .where(user -> user.getStatus() == User.Status.ACTIVE)
    .count();
```

Check if any user has a specific email domain.

```java
boolean exists = linqQueryBuilderFactory.createQuery(User.class)
    .any(user -> user.getEmail().endsWith("@example.com"));
```

### 7. Grouping (GroupBy)

Group users by status and count them.

```java
Map<Object, Long> counts = linqQueryBuilderFactory.createQuery(User.class)
    .groupBy(User::getStatus)
    .count();
```

## Supported Filter Operators

| Operator | Description | Example |
|----------|-------------|---------|
| EQUALS | Equal to | `field = value` |
| NOT_EQUALS | Not equal to | `field != value` |
| GREATER_THAN | Greater than | `field > value` |
| GREATER_EQUAL | Greater than or equal | `field >= value` |
| LESS_THAN | Less than | `field < value` |
| LESS_EQUAL | Less than or equal | `field <= value` |
| LIKE | Fuzzy matching | `field LIKE '%value%'` |
| NOT_LIKE | Not matching | `field NOT LIKE '%value%'` |
| IN | Within range | `field IN (value1, value2)` |
| NOT_IN | Not within range | `field NOT IN (
