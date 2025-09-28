/**
 * YmSQL - YAML-based SQL Operations Configuration
 *
 * <p><strong>EXPERIMENTAL:</strong> This package is currently in experimental status. APIs may
 * change without notice in future versions. Use with caution in production environments.
 *
 * <p>YmSQL provides a generalized solution for database operations through declarative YAML
 * configuration. Instead of writing repetitive SQL execution code, developers can define their
 * database queries and stored procedures in YAML files and invoke them programmatically with
 * type-safe parameter handling.
 *
 * <h2>Key Features</h2>
 *
 * <ul>
 *   <li><strong>Declarative Configuration:</strong> Define SQL operations in YAML files with
 *       comprehensive parameter specifications
 *   <li><strong>Type-Safe Parameters:</strong> Automatic parameter type conversion and validation
 *       with support for all JDBC data types
 *   <li><strong>Multi-Mode Parameters:</strong> Support for IN, OUT, INOUT, and REFCURSOR parameter
 *       modes for stored procedures
 *   <li><strong>Spring Integration:</strong> Seamless integration with Spring Boot through
 *       auto-configuration
 *   <li><strong>Resource Loading:</strong> Flexible resource loading with support for classpath,
 *       file system, and URL-based configurations
 *   <li><strong>Error Handling:</strong> Comprehensive error handling and validation with detailed
 *       logging
 * </ul>
 *
 * <h2>Package Structure</h2>
 *
 * <ul>
 *   <li>{@link com.turtleby.ymsql.core.model} - Core data models for SQL operation specifications
 *   <li>{@link com.turtleby.ymsql.core.dao} - Data access objects and registry for SQL operations
 *   <li>{@link com.turtleby.ymsql.spring.util} - Utility classes for loading and parsing YAML
 *       configurations
 *   <li>{@link com.turtleby.ymsql.spring.autoconfigure} - Spring Boot auto-configuration classes
 * </ul>
 *
 * <h2>Basic Usage</h2>
 *
 * <ol>
 *   <li>Define your SQL operations in a YAML file (e.g., {@code sql-operations.yml})
 *   <li>Configure YmSQL in your {@code application.yml} or {@code application.properties}
 *   <li>Inject the {@link com.turtleby.ymsql.core.dao.SqlOperationRegistry} to access your
 *       operations
 * </ol>
 *
 * <h2>YAML Configuration Format</h2>
 *
 * <pre>{@code
 * getUserById:
 *   sql: "SELECT * FROM users WHERE id = ?"
 *   operationType: "QUERY"
 *   parameters:
 *     - "integer:in"
 *
 * callStoredProcedure:
 *   sql: "CALL get_user_stats(?, ?, ?)"
 *   operationType: "PROCEDURE"
 *   parameters:
 *     - "integer:in"     # user_id
 *     - "integer:out"    # total_posts
 *     - "string:out"     # last_login
 * }</pre>
 *
 * <h2>Spring Configuration</h2>
 *
 * <pre>{@code
 * # application.yml
 * ymsql:
 *   enabled: true
 *   resource-path:
 *     - "classpath:sql-operations.yml"
 *     - "classpath:additional-queries.yml"
 * }</pre>
 *
 * <h2>Supported SQL Operation Types</h2>
 *
 * <ul>
 *   <li><strong>QUERY:</strong> SELECT statements that return result sets
 *   <li><strong>PROCEDURE:</strong> Stored procedure calls with support for output parameters
 * </ul>
 *
 * <h2>Supported Parameter Types</h2>
 *
 * <p>YmSQL supports all standard JDBC data types including:
 *
 * <ul>
 *   <li>Primitive types: {@code integer}, {@code long}, {@code boolean}, {@code double}
 *   <li>Text types: {@code string}, {@code text}, {@code char}, {@code varchar}
 *   <li>Date/time types: {@code date}, {@code time}, {@code timestamp}
 *   <li>Binary types: {@code bytes}, {@code blob}
 *   <li>Large objects: {@code clob}, {@code nclob}
 *   <li>Database-specific types: {@code array}, {@code ref}, {@code sqlxml}
 *   <li>PostgreSQL/Oracle types: {@code refcursor} for cursor parameters
 * </ul>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>All classes in this package are designed to be thread-safe. The registry and configuration
 * objects are immutable after construction, making them safe for concurrent access in
 * multi-threaded environments.
 *
 * @author Laxmikant Suryavanshi
 */
package com.turtleby.ymsql;
