# YmSQL - YAML-based SQL Operations Configuration

> **⚠️ EXPERIMENTAL:** This framework is currently in experimental status. APIs may change without notice in future versions. Use with caution in production environments.

## Overview

YmSQL provides a generalized solution for database operations through declarative YAML configuration. Instead of writing repetitive SQL execution code, developers can define their database queries and stored procedures in YAML files and invoke them programmatically with type-safe parameter handling.

## Key Features

- **📝 Declarative Configuration:** Define SQL operations in YAML files with comprehensive parameter specifications
- **🛡️ Type-Safe Parameters:** Automatic parameter type conversion and validation with support for all JDBC data types
- **🔄 Multi-Mode Parameters:** Support for IN, OUT, INOUT, and REFCURSOR parameter modes for stored procedures
- **🚀 Spring Integration:** Seamless integration with Spring Boot through auto-configuration
- **📂 Resource Loading:** Flexible resource loading with support for classpath, file system, and URL-based configurations
- **🚨 Error Handling:** Comprehensive error handling and validation with detailed logging

## Quick Start

### Maven Dependency

Add YmSQL to your project:

```xml
<dependency>
  <groupId>com.turtleby</groupId>
  <artifactId>ymsql</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 1. Define SQL Operations in YAML

Create a file `sql-operations.yml` in your `src/main/resources` directory:

```yaml
# Query operations
getUserById:
  sql: "SELECT id, name, email FROM users WHERE id = ?"
  operationType: "QUERY"
  parameters:
    - "integer:in"

getAllActiveUsers:
  sql: "SELECT * FROM users WHERE active = ?"
  operationType: "QUERY"
  parameters:
    - "boolean:in"

# Stored procedure operations
getUserStats:
  sql: "CALL get_user_statistics(?, ?, ?)"
  operationType: "PROCEDURE"
  parameters:
    - "integer:in" # user_id
    - "integer:out" # total_posts
    - "refcursor:out" # detailed_stats
```

### 2. Configure Spring Boot

Add configuration to your `application.yml`:

```yaml
ymsql:
  enabled: true
  resource-path:
    - "classpath:sql-operations.yml"
    - "classpath:additional-queries.yml" # Optional: multiple files
```

## Configuration Reference

### Supported Operation Types

| Type        | Example                            |
| ----------- | ---------------------------------- |
| `QUERY`     | `SELECT * FROM users WHERE id = ?` |
| `PROCEDURE` | `CALL get_user_stats(?, ?, ?)`     |

### Supported Parameter Types

YmSQL supports all standard JDBC data types:

#### Primitive Types

- `integer` - Integer values
- `long` - Long integer values
- `boolean` - Boolean true/false
- `double` - Double precision floating point
- `float` - Single precision floating point

#### Text Types

- `string` / `varchar` - Variable length strings
- `text` - Large text fields
- `char` - Fixed length strings

#### Date/Time Types

- `date` - SQL Date
- `time` - SQL Time
- `timestamp` - SQL Timestamp

#### Binary Types

- `bytes` - Byte arrays
- `blob` - Binary Large Objects

#### Large Objects

- `clob` - Character Large Objects
- `nclob` - National Character Large Objects

#### Database-Specific Types

- `array` - SQL Arrays
- `ref` - SQL REF types
- `sqlxml` - XML data
- `refcursor` - Oracle REF CURSOR (for output parameters)

### Parameter Modes

| Mode        | Description                 | Usage                                               |
| ----------- | --------------------------- | --------------------------------------------------- |
| `in`        | Input parameter (default)   | Query conditions, procedure inputs                  |
| `out`       | Output parameter            | Procedure return values                             |
| `inout`     | Input/Output parameter      | Procedure parameters that are both input and output |
| `refcursor` | REF CURSOR output parameter | For procedures returning result sets via cursors    |

## Package Structure

- **`com.turtleby.ymsql.model`** - Core data models for SQL operation specifications
- **`com.turtleby.ymsql.util`** - Utility classes for loading and parsing YAML configurations
- **`com.turtleby.ymsql.dao`** - Data access objects and registry for SQL operations
- **`com.turtleby.ymsql.autoconfigure`** - Spring Boot auto-configuration classes

## Thread Safety

All classes in YmSQL are designed to be thread-safe. The registry and configuration objects are immutable after construction, making them safe for concurrent access in multi-threaded environments.

## Error Handling

YmSQL provides comprehensive error handling and validation:

- **Configuration Validation:** Invalid YAML structure or missing required fields
- **Type Validation:** Unsupported parameter types or invalid type specifications
- **Resource Loading:** Missing or inaccessible YAML files
- **Runtime Validation:** Parameter count mismatches or type conversion errors

## Testing

YmSQL includes comprehensive test coverage with JUnit 5 and Mockito:

```bash
# Run all tests
./mvnw test -pl ymsql

# Run with coverage report
./mvnw clean test jacoco:report -pl ymsql
```

Coverage reports are generated in `target/site/jacoco/index.html`.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes with appropriate tests
4. Run code formatting: `./mvnw spotless:apply -pl ymsql`
5. Ensure all tests pass: `./mvnw test -pl ymsql`
6. Commit your changes (`git commit -m 'Add some amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Code Style

This project uses Spotless for code formatting. Run the following command before committing:

```bash
./mvnw spotless:apply -pl ymsql
```

---

**Disclaimer:** This is an experimental framework. While it has comprehensive test coverage and follows best practices, it should be thoroughly tested in your specific use case before production deployment.
