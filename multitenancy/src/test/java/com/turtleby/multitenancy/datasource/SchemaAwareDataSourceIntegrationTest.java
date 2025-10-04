package com.turtleby.multitenancy.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.turtleby.multitenancy.TestConstants;
import com.turtleby.multitenancy.context.TenantContext;
import com.turtleby.multitenancy.context.TenantContextHolder;
import com.turtleby.multitenancy.core.IsolationMode;
import com.turtleby.multitenancy.core.Tenant;

/**
 * Integration tests for {@link SchemaAwareDataSource} with PostgreSQL and Testcontainers.
 *
 * <p>Tests schema-based multi-tenancy functionality including:
 *
 * <ul>
 *   <li>Schema switching per tenant
 *   <li>Search path isolation
 *   <li>Connection pool integration
 *   <li>Cleanup and error handling
 * </ul>
 */
@Testcontainers
@DisplayName("SchemaAwareDataSource Integration Tests")
class SchemaAwareDataSourceIntegrationTest {

  @Container
  @SuppressWarnings("resource") // TestContainers handles resource lifecycle
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(TestConstants.POSTGRES_DOCKER_IMAGE)
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  private static DataSource baseDataSource;
  private SchemaAwareDataSource schemaAwareDataSource;

  @BeforeAll
  static void setUpDataSource() {
    // Configure HikariCP DataSource once for all tests
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(postgres.getJdbcUrl());
    config.setUsername(postgres.getUsername());
    config.setPassword(postgres.getPassword());
    config.setDriverClassName(postgres.getDriverClassName());
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);

    baseDataSource = new HikariDataSource(config);
  }

  @AfterAll
  static void tearDownDataSource() throws Exception {
    if (baseDataSource instanceof HikariDataSource) {
      ((HikariDataSource) baseDataSource).close();
    }
  }

  @BeforeEach
  void setUp() throws SQLException {
    schemaAwareDataSource = new SchemaAwareDataSource(baseDataSource);
    createTestSchemas();
    setupTestData();
  }

  @AfterEach
  void tearDown() {
    // Clear tenant context after each test
    TenantContextHolder.clear();
  }

  @Test
  @DisplayName("Should switch schemas based on tenant context")
  void shouldSwitchSchemasBasedOnTenantContext() throws SQLException {
    // Given: Set tenant context for tenant_a
    setTenantContext("tenant_a", "tenant_a_schema");

    // When: Get connection and query data
    try (Connection connection = schemaAwareDataSource.getConnection();
        PreparedStatement ps =
            connection.prepareStatement("SELECT name FROM test_table WHERE id = 1");
        ResultSet rs = ps.executeQuery()) {

      // Then: Should get data from tenant_a_schema
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("name")).isEqualTo("Tenant A Data");
    }

    // Given: Switch to tenant_b
    setTenantContext("tenant_b", "tenant_b_schema");

    // When: Get connection and query data
    try (Connection connection = schemaAwareDataSource.getConnection();
        PreparedStatement ps =
            connection.prepareStatement("SELECT name FROM test_table WHERE id = 1");
        ResultSet rs = ps.executeQuery()) {

      // Then: Should get data from tenant_b_schema
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("name")).isEqualTo("Tenant B Data");
    }
  }

  @Test
  @DisplayName("Should isolate data between tenants")
  void shouldIsolateDataBetweenTenants() throws SQLException {
    // Given: Insert data for tenant_a
    setTenantContext("tenant_a", "tenant_a_schema");
    try (Connection connection = schemaAwareDataSource.getConnection();
        PreparedStatement ps =
            connection.prepareStatement(
                "INSERT INTO test_table (id, name) VALUES (2, 'Private A Data')")) {
      ps.executeUpdate();
    }

    // When: Switch to tenant_b and try to access the data
    setTenantContext("tenant_b", "tenant_b_schema");
    try (Connection connection = schemaAwareDataSource.getConnection();
        PreparedStatement ps =
            connection.prepareStatement("SELECT name FROM test_table WHERE id = 2");
        ResultSet rs = ps.executeQuery()) {

      // Then: Should not see tenant_a's data
      assertThat(rs.next()).isFalse();
    }

    // And: Tenant A should still see its data
    setTenantContext("tenant_a", "tenant_a_schema");
    try (Connection connection = schemaAwareDataSource.getConnection();
        PreparedStatement ps =
            connection.prepareStatement("SELECT name FROM test_table WHERE id = 2");
        ResultSet rs = ps.executeQuery()) {

      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("name")).isEqualTo("Private A Data");
    }
  }

  @Test
  @DisplayName("Should reset search path when connection is closed")
  void shouldResetSearchPathWhenConnectionIsClosed() throws SQLException {
    // Given: Set tenant context and get a connection
    setTenantContext("tenant_a", "tenant_a_schema");

    Connection tenantConnection = schemaAwareDataSource.getConnection();

    // When: Close the tenant-aware connection
    tenantConnection.close();

    // Then: Get a new raw connection and verify search_path is reset
    try (Connection rawConnection = baseDataSource.getConnection();
        PreparedStatement ps = rawConnection.prepareStatement("SHOW search_path");
        ResultSet rs = ps.executeQuery()) {

      assertThat(rs.next()).isTrue();
      String searchPath = rs.getString(1);
      // Should be back to default (public or "$user", public)
      assertThat(searchPath).contains("public");
      assertThat(searchPath).doesNotContain("tenant_a_schema");
    }
  }

  @Test
  @DisplayName("Should throw exception when tenant context is null")
  void shouldThrowExceptionWhenTenantContextIsNull() {
    // Given: No tenant context set
    TenantContextHolder.clear();

    // When/Then: Should throw IllegalStateException
    assertThatThrownBy(() -> schemaAwareDataSource.getConnection())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No tenant context set in current thread");
  }

  @Test
  @DisplayName("Should throw exception when tenant is null")
  void shouldThrowExceptionWhenTenantIsNull() {
    // Given: Tenant context with null tenant
    TenantContextHolder.setContext(new TenantContext(null));

    // When/Then: Should throw IllegalStateException
    assertThatThrownBy(() -> schemaAwareDataSource.getConnection())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No tenant set in current context");
  }

  @Test
  @DisplayName("Should throw exception when tenant has no schema")
  void shouldThrowExceptionWhenTenantHasNoSchema() {
    // Given: Tenant without schema
    Tenant tenantWithoutSchema = new TestTenant("tenant_no_schema", null);
    TenantContextHolder.setContext(new TenantContext(tenantWithoutSchema));

    // When/Then: Should throw IllegalStateException
    assertThatThrownBy(() -> schemaAwareDataSource.getConnection())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Tenant tenant_no_schema does not have a schema set");
  }

  @Test
  @DisplayName("Should reject invalid schema names")
  void shouldRejectInvalidSchemaNames() {
    // Given: Tenant with invalid schema name
    Tenant tenantWithInvalidSchema = new TestTenant("tenant_invalid", "invalid-schema;DROP TABLE");
    TenantContextHolder.setContext(new TenantContext(tenantWithInvalidSchema));

    // When/Then: Should throw IllegalArgumentException
    assertThatThrownBy(() -> schemaAwareDataSource.getConnection())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sanitization check failed for schema");
  }

  @Test
  @DisplayName("Should work with connection pools")
  void shouldWorkWithConnectionPools() throws SQLException {
    // Given: Set tenant context
    setTenantContext("tenant_a", "tenant_a_schema");

    // When: Get multiple connections (simulating connection pool usage)
    for (int i = 0; i < 5; i++) {
      try (Connection connection = schemaAwareDataSource.getConnection();
          PreparedStatement ps =
              connection.prepareStatement("SELECT name FROM test_table WHERE id = 1");
          ResultSet rs = ps.executeQuery()) {

        // Then: Each connection should be properly configured
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("name")).isEqualTo("Tenant A Data");
      }
    }
  }

  @Test
  @DisplayName("Should handle both connection methods")
  void shouldHandleBothConnectionMethods() throws SQLException {
    // Given: Set tenant context
    setTenantContext("tenant_a", "tenant_a_schema");

    // When: Get connection without credentials (should work)
    try (Connection connection = schemaAwareDataSource.getConnection();
        PreparedStatement ps =
            connection.prepareStatement("SELECT name FROM test_table WHERE id = 1");
        ResultSet rs = ps.executeQuery()) {

      // Then: Should work correctly
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("name")).isEqualTo("Tenant A Data");
    }

    // Note: Testing getConnection(username, password) is skipped because HikariCP doesn't support
    // it. This is expected behavior and documented in HikariCP documentation
  }

  @Test
  @DisplayName("Should optimize subsequent connections with same schema")
  void shouldOptimizeSubsequentConnectionsWithSameSchema() throws SQLException {
    // Given: Set tenant context
    setTenantContext("tenant_a", "tenant_a_schema");

    // When: Get first connection (should set schema)
    Connection connection1 = schemaAwareDataSource.getConnection();

    // Get second connection (should reuse schema setting if optimized)
    Connection connection2 = schemaAwareDataSource.getConnection();

    // Then: Both connections should work correctly
    try (PreparedStatement ps1 =
            connection1.prepareStatement("SELECT name FROM test_table WHERE id = 1");
        ResultSet rs1 = ps1.executeQuery();
        PreparedStatement ps2 =
            connection2.prepareStatement("SELECT name FROM test_table WHERE id = 1");
        ResultSet rs2 = ps2.executeQuery()) {

      assertThat(rs1.next()).isTrue();
      assertThat(rs1.getString("name")).isEqualTo("Tenant A Data");

      assertThat(rs2.next()).isTrue();
      assertThat(rs2.getString("name")).isEqualTo("Tenant A Data");
    } finally {
      connection1.close();
      connection2.close();
    }
  }

  private void createTestSchemas() throws SQLException {
    try (Connection connection = baseDataSource.getConnection();
        Statement stmt = connection.createStatement()) {

      // Create tenant schemas
      stmt.execute("CREATE SCHEMA IF NOT EXISTS tenant_a_schema");
      stmt.execute("CREATE SCHEMA IF NOT EXISTS tenant_b_schema");

      // Create test tables in each schema
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS tenant_a_schema.test_table (id INT PRIMARY KEY, name"
              + " VARCHAR(100))");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS tenant_b_schema.test_table (id INT PRIMARY KEY, name"
              + " VARCHAR(100))");
    }
  }

  private void setupTestData() throws SQLException {
    try (Connection connection = baseDataSource.getConnection();
        Statement stmt = connection.createStatement()) {

      // Insert test data for each tenant
      stmt.execute(
          "INSERT INTO tenant_a_schema.test_table (id, name) VALUES (1, 'Tenant A Data') ON"
              + " CONFLICT (id) DO NOTHING");
      stmt.execute(
          "INSERT INTO tenant_b_schema.test_table (id, name) VALUES (1, 'Tenant B Data') ON"
              + " CONFLICT (id) DO NOTHING");
    }
  }

  private void setTenantContext(String tenantId, String schema) {
    TestTenant tenant = new TestTenant(tenantId, schema);
    TenantContextHolder.setContext(new TenantContext(tenant));
  }

  /** Test implementation of Tenant interface for testing purposes. */
  private static class TestTenant implements Tenant {
    private final String tenantId;
    private final String schema;

    public TestTenant(String tenantId, String schema) {
      this.tenantId = tenantId;
      this.schema = schema;
    }

    @Override
    public String getTenantId() {
      return tenantId;
    }

    @Override
    public IsolationMode getIsolationMode() {
      return IsolationMode.SCHEMA;
    }

    @Override
    public String getSchema() {
      return schema;
    }

    @Override
    public String getDatabase() {
      return null; // Not used in schema isolation mode
    }
  }
}
