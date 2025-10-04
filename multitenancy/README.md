# Multitenancy Library

A Spring library providing PostgreSQL schema-based multitenancy support with automatic tenant context management and database isolation.

## Overview

This library enables SaaS applications to support multiple tenants by using PostgreSQL schemas to isolate tenant data. Each tenant gets their own schema within a shared database, providing logical data separation while maintaining operational efficiency.

## Features

### ✅ Core Components

- **Schema-based Tenant Isolation**: Each tenant operates within their own PostgreSQL schema
- **Thread-local Context Management**: Automatic tenant context propagation across request threads
- **HTTP Header Resolution**: Extract tenant information from HTTP requests
- **DataSource Integration**: Transparent schema switching at the connection level
- **Connection Pool Optimization**: Performance optimizations to minimize schema-setting overhead

## Architecture

```
┌─────────────────┐    ┌───────────────────┐    ┌─────────────────┐
│   HTTP Request  │ -> │ TenantInterceptor │ -> │ TenantResolver  │
│ X-Tenant-ID: A  │    │                   │    │                 │
└─────────────────┘    └───────────────────┘    └─────────────────┘
                                |
                                v
                    ┌─────────────────────┐
                    │ TenantContextHolder │ (Thread-Local)
                    │ Current: Tenant A   │
                    └─────────────────────┘
                                |
                                v
                    ┌─────────────────────┐    ┌─────────────────┐
                    │SchemaAwareDataSource│ -> │ SET search_path │
                    │                     │    │ TO tenant_a     │
                    └─────────────────────┘    └─────────────────┘
```

## Components

### Core Interfaces

#### `Tenant`
```java
public interface Tenant {
  String getTenantId();             // Unique tenant identifier
  IsolationMode getIsolationMode(); // SCHEMA or DATABASE
  String getSchema();               // PostgreSQL schema name
  String getDatabase();             // Database name (nullable for schema mode)
}
```

#### `TenantResolver<T>`
```java
@FunctionalInterface
public interface TenantResolver<T> {
  String resolveTenantId(T context);  // Extract tenant ID from context
}
```

#### `TenantDetailsService`
```java
@FunctionalInterface
public interface TenantDetailsService {
  Tenant getTenantByTenantId(String tenantId) throws TenantNotFoundException;
}
```

### Implementation Classes

#### `TenantContextHolder`
Thread-local holder for current tenant context with inheritance support:
```java
// Set tenant context
TenantContextHolder.setContext(new TenantContext(tenant));

// Get current tenant
TenantContext context = TenantContextHolder.getContext();
Tenant tenant = context.getTenant();

// Clear context
TenantContextHolder.clear();
```

#### `HttpHeaderTenantResolver`
Extracts tenant ID from HTTP headers:
```java
public class HttpHeaderTenantResolver implements TenantResolver<HttpServletRequest> {
  // Resolves tenant from X-Tenant-ID header (configurable)
}
```

#### `SchemaAwareDataSource`
PostgreSQL-specific DataSource wrapper that automatically sets `search_path`:
```java
public DataSource schemaAwareDataSource(DataSource dataSource) {
  return new SchemaAwareDataSource(dataSource);
}
```

**Key Features:**
- Automatic schema switching based on current tenant context
- Connection-level optimization to avoid redundant `SET search_path` calls
- Proper cleanup when connections are returned to the pool
- SQL injection protection for schema names
- Performance monitoring via debug logging

#### `TenantInterceptor`
Spring MVC interceptor that manages tenant lifecycle:
```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
  String tenantId = tenantResolver.resolveTenantId(request);
  Tenant tenant = tenantDetailsService.getTenantByTenantId(tenantId);
  TenantContextHolder.setContext(new TenantContext(tenant));
  MDC.put("tenantId", tenantId);  // For logging
  return true;
}

@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
  TenantContextHolder.clear();
  MDC.remove("tenantId");
}
```

## Usage Example

### 1. Implement TenantDetailsService
```java
public class DatabaseTenantDetailsService implements TenantDetailsService {
  @Override
  public Tenant getTenantByTenantId(String tenantId) throws TenantNotFoundException {
    // Query your tenant registry database
    TenantEntity entity = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

    return new TenantImpl(entity.getTenantId(), entity.getSchemaName());
  }
}
```

### 2. Make HTTP Requests
```bash
curl -H "X-Tenant-ID: tenant-a" http://localhost:8080/api/customers
# Automatically queries from tenant_a schema

curl -H "X-Tenant-ID: tenant-b" http://localhost:8080/api/customers  
# Automatically queries from tenant_b schema
```

### 3. Database Schema Structure
```sql
-- Shared database with multiple schemas
CREATE SCHEMA tenant_a;
CREATE SCHEMA tenant_b;

-- Each tenant has their own tables
CREATE TABLE tenant_a.customers (id SERIAL PRIMARY KEY, name VARCHAR(255));
CREATE TABLE tenant_b.customers (id SERIAL PRIMARY KEY, name VARCHAR(255));

-- Data is completely isolated
INSERT INTO tenant_a.customers (name) VALUES ('Customer A1');
INSERT INTO tenant_b.customers (name) VALUES ('Customer B1');
```

## Performance Optimizations

### Connection State Tracking
The `SchemaAwareDataSource` tracks the current schema for each connection to avoid unnecessary `SET search_path` operations:

```java
// First request for tenant-a: executes SET search_path TO tenant_a
Connection conn1 = dataSource.getConnection(); 

// Subsequent requests with same tenant: skips SET operation
Connection conn2 = dataSource.getConnection(); // No SET needed
```

### Connection Pool Integration
Works seamlessly with HikariCP and other connection pools:
- Schema state is tracked per physical connection
- Automatic cleanup when connections are returned to pool
- Minimal overhead for schema switching operations

### Integration Test Example
```java
@Testcontainers
class SchemaAwareDataSourceIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5-alpine3.22");
    
  @Test
  void shouldIsolateDataBetweenTenants() throws SQLException {
    // Given: Two tenants with separate schemas
    setTenantContext("tenant-a", "tenant_a_schema");

    // When: Insert data for tenant A
    try (Connection conn = schemaAwareDataSource.getConnection()) {
        // Data goes to tenant_a_schema.test_table
    }

    // Then: Tenant B cannot see tenant A's data
    setTenantContext("tenant-b", "tenant_b_schema");
    // Queries go to tenant_b_schema.test_table
  }
}
```

## Error Handling

### Graceful Degradation
- `TenantNotFoundException`: Returns null tenant, application can handle gracefully
- Missing HTTP headers: Continues with null tenant context
- Schema validation: Prevents SQL injection with identifier validation
- Connection errors: Proper cleanup and error propagation

### Logging Integration
- SLF4J integration with configurable debug logging
- MDC support for tenant-aware log aggregation
- Performance metrics for schema switching operations

## Dependencies

### Runtime Dependencies (Provided)
```xml
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-webmvc</artifactId>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-jdbc</artifactId>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <scope>provided</scope>
</dependency>
```

### Test Dependencies
```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.zaxxer</groupId>
  <artifactId>HikariCP</artifactId>
  <scope>test</scope>
</dependency>
```

## Database Compatibility

- **PostgreSQL**: Full support with schema-based isolation
- **Other databases**: Interface allows for future database-specific implementations

## Thread Safety

- **TenantContextHolder**: Uses `InheritableThreadLocal` for thread-safe context propagation
- **SchemaAwareDataSource**: Thread-safe with connection-specific state tracking
- **All components**: Designed for concurrent multi-tenant environments

## Limitations

1. **PostgreSQL Specific**: Current implementation uses PostgreSQL `search_path`
2. **Schema-based Only**: Currently supports schema isolation mode only
3. **HTTP Headers**: Primary tenant resolution method (extensible via `TenantResolver`)

## Future Enhancements

- [ ] Spring Boot auto-configuration support
- [ ] Database-based isolation mode support
- [ ] JWT-based tenant resolution
- [ ] Subdomain-based tenant resolution
- [ ] MySQL/SQL Server support
- [ ] Tenant-specific connection pooling
- [ ] Dynamic schema creation/migration support

## Contributing

1. All changes must include comprehensive tests
2. Integration tests should use TestContainers for real database testing
3. Follow the existing code style and documentation patterns
4. Performance optimizations should include benchmarks
