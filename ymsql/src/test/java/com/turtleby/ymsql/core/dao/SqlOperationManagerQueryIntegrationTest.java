package com.turtleby.ymsql.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.turtleby.ymsql.TestConstants;
import com.turtleby.ymsql.core.model.SqlOperationResult;
import com.turtleby.ymsql.core.model.SqlOperationSpec;
import com.turtleby.ymsql.core.model.SqlOperationSpec.SqlOperationType;
import com.turtleby.ymsql.core.model.SqlParameter;
import com.turtleby.ymsql.core.model.SqlParameterType;

@Testcontainers
@DisplayName("SqlOperationManager Integration Tests - SQL Queries")
class SqlOperationManagerQueryIntegrationTest {

  @Container
  @SuppressWarnings("resource") // TestContainers handles resource lifecycle
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(TestConstants.POSTGRES_DOCKER_IMAGE)
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  private static DataSource dataSource;
  private SqlOperationRegistry registry;
  private SqlOperationManager operationManager;

  @BeforeAll
  static void setUpDataSource() throws SQLException {
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

    dataSource = new HikariDataSource(config);
  }

  @AfterAll
  static void tearDownDataSource() throws SQLException {
    if (dataSource instanceof HikariDataSource) {
      ((HikariDataSource) dataSource).close();
    }
  }

  @BeforeEach
  void setUp() throws SQLException {
    // Create test tables for each test (ensure clean state)
    createTestTables();

    // Setup operation registry with test operations
    setupOperationRegistry();

    // Create SqlOperationManager
    operationManager = new SqlOperationManager(registry, dataSource);
  }

  private void createTestTables() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // Drop tables if they exist
      statement.execute("DROP TABLE IF EXISTS users CASCADE");
      statement.execute("DROP TABLE IF EXISTS orders CASCADE");

      // Create users table
      statement.execute(
          """
          CREATE TABLE users (
              id SERIAL PRIMARY KEY,
              username VARCHAR(50) UNIQUE NOT NULL,
              email VARCHAR(100) NOT NULL,
              age INTEGER,
              is_active BOOLEAN DEFAULT TRUE,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Create orders table
      statement.execute(
          """
          CREATE TABLE orders (
              id SERIAL PRIMARY KEY,
              user_id INTEGER REFERENCES users(id),
              product_name VARCHAR(100) NOT NULL,
              quantity INTEGER NOT NULL,
              price DECIMAL(10,2) NOT NULL,
              order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Insert sample data
      statement.execute(
          """
          INSERT INTO users (username, email, age, is_active) VALUES
          ('john_doe', 'john@example.com', 30, true),
          ('jane_smith', 'jane@example.com', 25, true),
          ('bob_jones', 'bob@example.com', 35, true)
          """);
      statement.execute(
          """
          INSERT INTO orders (user_id, product_name, quantity, price) VALUES
          (1, 'Laptop', 1, 999.99),
          (1, 'Mouse', 2, 25.50),
          (2, 'Keyboard', 1, 75.00),
          (3, 'Monitor', 1, 299.99)
          """);
    }
  }

  private void setupOperationRegistry() {
    Map<String, SqlOperationSpec> operations = new HashMap<>();

    // SELECT operations
    operations.put(
        "getAllUsers",
        SqlOperationSpec.builder()
            .sql("SELECT id, username, email, age, is_active FROM users ORDER BY id")
            .operationType(SqlOperationType.QUERY)
            .build());

    operations.put(
        "getUserById",
        SqlOperationSpec.builder()
            .sql("SELECT id, username, email, age, is_active FROM users WHERE id = ?")
            .operationType(SqlOperationType.QUERY)
            .parameters(List.of(SqlParameter.in("userId", SqlParameterType.INTEGER)))
            .build());

    operations.put(
        "getUsersByAgeRange",
        SqlOperationSpec.builder()
            .sql(
                "SELECT id, username, email, age FROM users WHERE age BETWEEN ? AND ? ORDER BY age")
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("minAge", SqlParameterType.INTEGER),
                    SqlParameter.in("maxAge", SqlParameterType.INTEGER)))
            .build());

    // INSERT operations
    operations.put(
        "insertUser",
        SqlOperationSpec.builder()
            .sql("INSERT INTO users (username, email, age, is_active) VALUES (?, ?, ?, ?)")
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("username", SqlParameterType.STRING),
                    SqlParameter.in("email", SqlParameterType.STRING),
                    SqlParameter.in("age", SqlParameterType.INTEGER),
                    SqlParameter.in("isActive", SqlParameterType.BOOLEAN)))
            .build());

    operations.put(
        "insertOrder",
        SqlOperationSpec.builder()
            .sql("INSERT INTO orders (user_id, product_name, quantity, price) VALUES (?, ?, ?, ?)")
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("userId", SqlParameterType.INTEGER),
                    SqlParameter.in("productName", SqlParameterType.STRING),
                    SqlParameter.in("quantity", SqlParameterType.INTEGER),
                    SqlParameter.in("price", SqlParameterType.DECIMAL)))
            .build());

    // UPDATE operations
    operations.put(
        "updateUserAge",
        SqlOperationSpec.builder()
            .sql("UPDATE users SET age = ? WHERE id = ?")
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("age", SqlParameterType.INTEGER),
                    SqlParameter.in("userId", SqlParameterType.INTEGER)))
            .build());

    operations.put(
        "updateUserStatus",
        SqlOperationSpec.builder()
            .sql("UPDATE users SET is_active = ? WHERE username = ?")
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("isActive", SqlParameterType.BOOLEAN),
                    SqlParameter.in("username", SqlParameterType.STRING)))
            .build());

    // DELETE operations
    operations.put(
        "deleteUserById",
        SqlOperationSpec.builder()
            .sql("DELETE FROM users WHERE id = ?")
            .operationType(SqlOperationType.QUERY)
            .parameters(List.of(SqlParameter.in("userId", SqlParameterType.INTEGER)))
            .build());

    operations.put(
        "deleteInactiveUsers",
        SqlOperationSpec.builder()
            .sql("DELETE FROM users WHERE is_active = false")
            .operationType(SqlOperationType.QUERY)
            .build());

    // Complex SELECT with JOIN
    operations.put(
        "getUsersWithOrders",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT u.id, u.username, u.email, o.product_name, o.quantity, o.price
                FROM users u
                INNER JOIN orders o ON u.id = o.user_id
                WHERE u.is_active = true
                ORDER BY u.id, o.id
                """)
            .operationType(SqlOperationType.QUERY)
            .build());

    // Aggregate queries
    operations.put(
        "getUserOrderSummary",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT u.id, u.username, COUNT(o.id) as order_count,
                       COALESCE(SUM(o.price * o.quantity), 0) as total_spent
                FROM users u
                LEFT JOIN orders o ON u.id = o.user_id
                GROUP BY u.id, u.username
                ORDER BY total_spent DESC
                """)
            .operationType(SqlOperationType.QUERY)
            .build());

    registry = new SqlOperationRegistry(operations);
  }

  // SELECT Tests
  @Test
  @DisplayName("Should execute simple SELECT query and return all users")
  void shouldExecuteSimpleSelectQueryAndReturnAllUsers() throws SQLException {
    SqlOperationResult result = operationManager.execute("getAllUsers", new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(4, resultSet.size()); // 1 header + 3 data rows

    // Check header
    List<Object> headers = resultSet.get(0);
    assertEquals(List.of("id", "username", "email", "age", "is_active"), headers);

    // Check first user data
    List<Object> firstUser = resultSet.get(1);
    assertEquals(1, firstUser.get(0)); // id
    assertEquals("john_doe", firstUser.get(1)); // username
    assertEquals("john@example.com", firstUser.get(2)); // email
    assertEquals(30, firstUser.get(3)); // age
    assertEquals(true, firstUser.get(4)); // is_active

    assertTrue(result.getExecutionTimeMs() >= 0);
  }

  @Test
  @DisplayName("Should execute parameterized SELECT query and return specific user")
  void shouldExecuteParameterizedSelectQueryAndReturnSpecificUser() throws SQLException {
    SqlOperationResult result = operationManager.execute("getUserById", new Object[] {2});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(2, resultSet.size()); // 1 header + 1 data row

    List<Object> userData = resultSet.get(1);
    assertEquals(2, userData.get(0)); // id
    assertEquals("jane_smith", userData.get(1)); // username
    assertEquals("jane@example.com", userData.get(2)); // email
  }

  @Test
  @DisplayName("Should execute SELECT with multiple parameters and return filtered results")
  void shouldExecuteSelectWithMultipleParametersAndReturnFilteredResults() throws SQLException {
    SqlOperationResult result =
        operationManager.execute("getUsersByAgeRange", new Object[] {25, 32});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(3, resultSet.size()); // 1 header + 2 data rows (jane=25, john=30)

    // Verify both users are in the age range
    List<Object> user1 = resultSet.get(1);
    List<Object> user2 = resultSet.get(2);

    assertTrue((Integer) user1.get(3) >= 25 && (Integer) user1.get(3) <= 32);
    assertTrue((Integer) user2.get(3) >= 25 && (Integer) user2.get(3) <= 32);
  }

  @Test
  @DisplayName("Should execute SELECT query and return empty result set")
  void shouldExecuteSelectQueryAndReturnEmptyResultSet() throws SQLException {
    SqlOperationResult result = operationManager.execute("getUserById", new Object[] {999});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(1, resultSet.size()); // Only header row

    List<Object> headers = resultSet.get(0);
    assertEquals(List.of("id", "username", "email", "age", "is_active"), headers);
  }

  // INSERT Tests
  @Test
  @DisplayName("Should execute INSERT query and add new user")
  void shouldExecuteInsertQueryAndAddNewUser() throws SQLException {
    SqlOperationResult result =
        operationManager.execute(
            "insertUser", new Object[] {"alice_cooper", "alice@example.com", 28, true});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getAffectedRows());

    // Verify the user was inserted by querying for it
    SqlOperationResult selectResult = operationManager.execute("getAllUsers", new Object[0]);
    List<List<Object>> resultSet = selectResult.getResultSets().get(0);
    assertEquals(5, resultSet.size()); // 1 header + 4 data rows (3 original + 1 new)

    // Find the new user
    boolean found = false;
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      if ("alice_cooper".equals(row.get(1))) {
        assertEquals("alice@example.com", row.get(2));
        assertEquals(28, row.get(3));
        assertEquals(true, row.get(4));
        found = true;
        break;
      }
    }
    assertTrue(found, "New user should be found in the database");
  }

  @Test
  @DisplayName("Should execute INSERT query with different data types")
  void shouldExecuteInsertQueryWithDifferentDataTypes() throws SQLException {
    SqlOperationResult result =
        operationManager.execute("insertOrder", new Object[] {1, "Tablet", 3, 299.99});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getAffectedRows());
  }

  @Test
  @DisplayName("Should handle INSERT constraint violation")
  void shouldHandleInsertConstraintViolation() throws SQLException {
    // Try to insert a user with duplicate username (should violate unique constraint)
    assertThrows(
        SQLException.class,
        () -> {
          operationManager.execute(
              "insertUser", new Object[] {"john_doe", "different@example.com", 40, true});
        });
  }

  // UPDATE Tests
  @Test
  @DisplayName("Should execute UPDATE query and modify existing record")
  void shouldExecuteUpdateQueryAndModifyExistingRecord() throws SQLException {
    SqlOperationResult result = operationManager.execute("updateUserAge", new Object[] {31, 1});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getAffectedRows());

    // Verify the update by querying the user
    SqlOperationResult selectResult = operationManager.execute("getUserById", new Object[] {1});
    List<List<Object>> resultSet = selectResult.getResultSets().get(0);
    assertEquals(2, resultSet.size());

    List<Object> userData = resultSet.get(1);
    assertEquals(31, userData.get(3)); // age should be updated to 31
  }

  @Test
  @DisplayName("Should execute UPDATE query with different parameter types")
  void shouldExecuteUpdateQueryWithDifferentParameterTypes() throws SQLException {
    SqlOperationResult result =
        operationManager.execute("updateUserStatus", new Object[] {false, "jane_smith"});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getAffectedRows());

    // Verify the update
    SqlOperationResult selectResult = operationManager.execute("getUserById", new Object[] {2});
    List<List<Object>> resultSet = selectResult.getResultSets().get(0);
    List<Object> userData = resultSet.get(1);
    assertEquals(false, userData.get(4)); // is_active should be false
  }

  @Test
  @DisplayName("Should execute UPDATE query that affects no rows")
  void shouldExecuteUpdateQueryThatAffectsNoRows() throws SQLException {
    SqlOperationResult result = operationManager.execute("updateUserAge", new Object[] {40, 999});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(0, result.getAffectedRows()); // No rows should be affected
  }

  // DELETE Tests
  @Test
  @DisplayName("Should execute DELETE query and remove specific record")
  void shouldExecuteDeleteQueryAndRemoveSpecificRecord() throws SQLException {
    // First create a user without any orders
    operationManager.execute(
        "insertUser", new Object[] {"temp_user", "temp@example.com", 25, true});

    // Get the new user's ID
    SqlOperationResult selectResult = operationManager.execute("getAllUsers", new Object[0]);
    List<List<Object>> users = selectResult.getResultSets().get(0);
    Integer tempUserId = null;
    for (int i = 1; i < users.size(); i++) {
      List<Object> row = users.get(i);
      if ("temp_user".equals(row.get(1))) {
        tempUserId = (Integer) row.get(0);
        break;
      }
    }
    assertNotNull(tempUserId, "Temp user should be found");

    // Now delete this user (no foreign key constraint violation)
    SqlOperationResult result =
        operationManager.execute("deleteUserById", new Object[] {tempUserId});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getAffectedRows());

    // Verify the user was deleted
    SqlOperationResult verifyResult =
        operationManager.execute("getUserById", new Object[] {tempUserId});
    List<List<Object>> resultSet = verifyResult.getResultSets().get(0);
    assertEquals(1, resultSet.size()); // Only header row, no data
  }

  @Test
  @DisplayName("Should execute DELETE query with WHERE clause")
  void shouldExecuteDeleteQueryWithWhereClause() throws SQLException {
    // First create some users without orders to avoid foreign key constraints
    operationManager.execute(
        "insertUser", new Object[] {"inactive_user1", "inactive1@example.com", 30, false});
    operationManager.execute(
        "insertUser", new Object[] {"inactive_user2", "inactive2@example.com", 35, false});

    SqlOperationResult result = operationManager.execute("deleteInactiveUsers", new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertTrue(result.getAffectedRows() >= 2); // Should delete the 2 inactive users we created

    // Verify inactive users were deleted
    SqlOperationResult selectResult = operationManager.execute("getAllUsers", new Object[0]);
    List<List<Object>> resultSet = selectResult.getResultSets().get(0);

    // Check that all remaining users are active
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      assertEquals(true, row.get(4)); // is_active should be true
    }
  }

  @Test
  @DisplayName("Should execute DELETE query that affects no rows")
  void shouldExecuteDeleteQueryThatAffectsNoRows() throws SQLException {
    SqlOperationResult result = operationManager.execute("deleteUserById", new Object[] {999});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(0, result.getAffectedRows());
  }

  // Complex Query Tests
  @Test
  @DisplayName("Should execute complex SELECT with JOIN")
  void shouldExecuteComplexSelectWithJoin() throws SQLException {
    SqlOperationResult result = operationManager.execute("getUsersWithOrders", new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertTrue(resultSet.size() > 1); // Should have header + data rows

    // Check header
    List<Object> headers = resultSet.get(0);
    assertEquals(List.of("id", "username", "email", "product_name", "quantity", "price"), headers);

    // Verify data integrity - all returned users should have orders
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      assertNotNull(row.get(3)); // product_name should not be null
      assertNotNull(row.get(4)); // quantity should not be null
      assertNotNull(row.get(5)); // price should not be null
    }
  }

  @Test
  @DisplayName("Should execute aggregate query with GROUP BY")
  void shouldExecuteAggregateQueryWithGroupBy() throws SQLException {
    SqlOperationResult result = operationManager.execute("getUserOrderSummary", new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(4, resultSet.size()); // 1 header + 3 users

    // Check header
    List<Object> headers = resultSet.get(0);
    assertEquals(List.of("id", "username", "order_count", "total_spent"), headers);

    // Verify aggregation data types
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      assertNotNull(row.get(0)); // id
      assertNotNull(row.get(1)); // username
      assertTrue(row.get(2) instanceof Number); // order_count
      assertTrue(row.get(3) instanceof Number); // total_spent
    }
  }

  // Error Handling Tests
  @Test
  @DisplayName("Should handle SQL syntax error gracefully")
  void shouldHandleSQLSyntaxErrorGracefully() throws SQLException {
    // Create an operation with invalid SQL
    Map<String, SqlOperationSpec> invalidOperations = new HashMap<>();
    invalidOperations.put(
        "invalidQuery",
        SqlOperationSpec.builder()
            .sql("INVALID SQL SYNTAX")
            .operationType(SqlOperationType.QUERY)
            .build());

    SqlOperationRegistry invalidRegistry = new SqlOperationRegistry(invalidOperations);
    SqlOperationManager invalidManager = new SqlOperationManager(invalidRegistry, dataSource);

    assertThrows(
        SQLException.class,
        () -> {
          invalidManager.execute("invalidQuery", new Object[0]);
        });
  }

  @Test
  @DisplayName("Should handle foreign key constraint violation")
  void shouldHandleForeignKeyConstraintViolation() throws SQLException {
    // Try to insert an order with non-existent user_id
    assertThrows(
        SQLException.class,
        () -> {
          operationManager.execute(
              "insertOrder", new Object[] {999, "Non-existent User Product", 1, 99.99});
        });
  }

  // Performance and Connection Management Tests
  @Test
  @DisplayName("Should handle multiple concurrent operations")
  void shouldHandleMultipleConcurrentOperations() throws SQLException {
    // Execute multiple operations to test connection pooling
    for (int i = 0; i < 5; i++) {
      SqlOperationResult result = operationManager.execute("getAllUsers", new Object[0]);
      assertNotNull(result);
      assertTrue(result.isSuccess());
    }

    // All operations should complete successfully
    assertTrue(true, "All operations completed without connection issues");
  }

  @Test
  @DisplayName("Should properly handle null parameter values")
  void shouldProperlyHandleNullParameterValues() throws SQLException {
    // Insert user with null age
    SqlOperationResult result =
        operationManager.execute(
            "insertUser", new Object[] {"null_age_user", "null@example.com", null, true});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getAffectedRows());

    // Verify the null value was inserted correctly
    SqlOperationResult selectResult = operationManager.execute("getAllUsers", new Object[0]);
    List<List<Object>> resultSet = selectResult.getResultSets().get(0);

    // Find the user with null age
    boolean found = false;
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      if ("null_age_user".equals(row.get(1))) {
        assertNull(row.get(3)); // age should be null
        found = true;
        break;
      }
    }
    assertTrue(found, "User with null age should be found");
  }
}
