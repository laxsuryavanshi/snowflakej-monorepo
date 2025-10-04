package com.turtleby.multitenancy.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.lang.NonNull;

import com.turtleby.multitenancy.context.TenantContext;
import com.turtleby.multitenancy.context.TenantContextHolder;
import com.turtleby.multitenancy.core.Tenant;

/**
 * PostgreSQL-specific DataSource wrapper that sets search_path per current tenant and resets on
 * close.
 *
 * <p>This implementation uses PostgreSQL-specific SQL commands ('SET search_path' and 'RESET
 * search_path') to provide schema-based multi-tenancy. Each connection is configured with the
 * appropriate tenant schema and automatically reset when returned to the pool.
 *
 * <p><strong>Performance Optimizations:</strong>
 *
 * <ul>
 *   <li>Tracks current schema per connection to avoid unnecessary SET operations
 *   <li>Uses input validation to prevent SQL injection
 *   <li>Provides timing metrics via debug logging
 * </ul>
 *
 * <p><strong>Database Compatibility:</strong> PostgreSQL only
 */
public class SchemaAwareDataSource extends DelegatingDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaAwareDataSource.class);

  private static final String DEFAULT_SCHEMA = "public";
  private static final String SET_SEARCH_PATH_SQL = "SET search_path TO %s, " + DEFAULT_SCHEMA;
  private static final String RESET_SEARCH_PATH_SQL = "RESET search_path";
  private static final String CONNECTION_SCHEMA_ATTRIBUTE = "current_tenant_schema";

  /** Wrap given dataSource. */
  public SchemaAwareDataSource(DataSource dataSource) {
    super(dataSource);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns a connection configured for the current tenant's database schema. The connection is
   * wrapped to automatically reset the search_path when closed.
   *
   * @return a tenant-aware database connection
   * @throws SQLException if a database access error occurs
   * @throws IllegalStateException if no tenant context is set or tenant has no schema
   */
  @Override
  @NonNull
  public Connection getConnection() throws SQLException {
    return wrapConnection(super.getConnection());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns a connection configured for the current tenant's database schema. The connection is
   * wrapped to automatically reset the search_path when closed.
   *
   * @param username the database user on whose behalf the connection is being made
   * @param password the user's password
   * @return a tenant-aware database connection
   * @throws SQLException if a database access error occurs
   * @throws IllegalStateException if no tenant context is set or tenant has no schema
   */
  @Override
  @NonNull
  public Connection getConnection(String username, String password) throws SQLException {
    return wrapConnection(super.getConnection(username, password));
  }

  /**
   * Wraps a database connection to provide tenant-aware schema switching.
   *
   * <p>This method configures the connection for the current tenant's schema and returns a proxy
   * that automatically resets the search_path when closed.
   *
   * @param connection the database connection to wrap
   * @return a wrapped connection with tenant-aware schema configuration
   * @throws SQLException if database schema configuration fails
   * @throws IllegalStateException if no tenant context is available
   */
  private Connection wrapConnection(Connection connection) throws SQLException {
    TenantContext context = TenantContextHolder.getContext();
    if (context == null) {
      throw new IllegalStateException("No tenant context set in current thread");
    }

    Tenant tenant = context.getTenant();
    if (tenant == null) {
      throw new IllegalStateException("No tenant set in current context");
    }

    String schema = tenant.getSchema();
    if (schema == null) {
      throw new IllegalStateException(
          "Tenant " + tenant.getTenantId() + " does not have a schema set");
    }

    setSchema(connection, schema);

    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[] {Connection.class},
            new SchemaAwareConnectionProxy(connection));
  }

  /**
   * Sets the PostgreSQL search_path for the given connection to the specified schema.
   *
   * <p>This method validates the schema name to prevent SQL injection attacks and uses a simple
   * statement to set the search_path. It also tracks the current schema to avoid unnecessary
   * database round trips.
   *
   * @param connection the database connection to configure
   * @param schema the schema name to set in the search_path
   * @throws SQLException if the database operation fails
   * @throws IllegalArgumentException if the schema name fails validation
   */
  private void setSchema(Connection connection, String schema) throws SQLException {
    if (!schema.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
      throw new IllegalArgumentException("Sanitization check failed for schema '" + schema + "'");
    }

    // Check if the connection already has the correct schema set
    String currentSchema = (String) connection.getClientInfo(CONNECTION_SCHEMA_ATTRIBUTE);
    if (schema.equals(currentSchema)) {
      LOGGER.debug("Connection already configured for schema: {}", schema);
      return;
    }

    LOGGER.debug("Setting PostgreSQL search_path to: {}", schema);
    long startTime = System.nanoTime();

    // The schema name has already been validated with regex, so this is safe
    String sql = String.format(SET_SEARCH_PATH_SQL, schema);
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(sql);

      // Track the current schema on this connection
      connection.setClientInfo(CONNECTION_SCHEMA_ATTRIBUTE, schema);

      if (LOGGER.isDebugEnabled()) {
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        LOGGER.debug("Schema switch to '{}' completed in {} ms", schema, durationMs);
      }
    } catch (SQLException e) {
      LOGGER.error("Failed to set search_path to schema '{}': {}", schema, e.getMessage());
      throw e;
    }
  }

  /**
   * Connection proxy that automatically resets the PostgreSQL search_path when closed.
   *
   * <p>This proxy intercepts the {@code close()} method call and executes "RESET search_path"
   * before delegating to the underlying connection. This ensures that connections returned to the
   * pool are in a clean state.
   */
  private static class SchemaAwareConnectionProxy implements InvocationHandler {

    private final Connection targetConnection;

    /**
     * Creates a new connection proxy.
     *
     * @param targetConnection the underlying connection to proxy
     */
    public SchemaAwareConnectionProxy(Connection targetConnection) {
      this.targetConnection = targetConnection;
    }

    /**
     * Intercepts method calls on the proxied connection.
     *
     * <p>If the {@code close()} method is called, this method first resets the PostgreSQL
     * search_path to its default state before closing the connection. All other method calls are
     * delegated directly to the underlying connection.
     *
     * @param proxy the proxy instance
     * @param method the method being invoked
     * @param args the method arguments
     * @return the result of the method invocation
     * @throws Throwable if the method invocation fails
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("close".equals(method.getName())) {
        // Reset search_path before closing
        try (Statement stmt = targetConnection.createStatement()) {
          stmt.execute(RESET_SEARCH_PATH_SQL);
          // Clear the schema tracking
          targetConnection.setClientInfo(CONNECTION_SCHEMA_ATTRIBUTE, null);
        } catch (SQLException e) {
          // Log the error but don't prevent connection close
          LOGGER.warn("Failed to reset search_path before closing connection", e);
        }
        // Always proceed with the actual close, even if reset failed
        return method.invoke(targetConnection, args);
      }
      return method.invoke(targetConnection, args);
    }
  }
}
