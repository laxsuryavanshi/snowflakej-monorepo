package com.turtleby.ymsql.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
@DisplayName("SqlOperationManager Integration Tests - Complex Queries")
class SqlOperationManagerComplexQueryIntegrationTest {

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

    // Setup operation registry with complex operations
    setupComplexOperationRegistry();

    // Create SqlOperationManager
    operationManager = new SqlOperationManager(registry, dataSource);
  }

  private void createTestTables() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // Drop tables if they exist
      statement.execute("DROP TABLE IF EXISTS transactions CASCADE");
      statement.execute("DROP TABLE IF EXISTS products CASCADE");
      statement.execute("DROP TABLE IF EXISTS categories CASCADE");
      statement.execute("DROP TABLE IF EXISTS customers CASCADE");
      statement.execute("DROP TABLE IF EXISTS financial_records CASCADE");
      statement.execute("DROP TABLE IF EXISTS batch_operations CASCADE");
      statement.execute("DROP TABLE IF EXISTS temporal_data CASCADE");

      // Create categories table
      statement.execute(
          """
          CREATE TABLE categories (
              id SERIAL PRIMARY KEY,
              name VARCHAR(100) UNIQUE NOT NULL,
              description TEXT,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Create customers table
      statement.execute(
          """
          CREATE TABLE customers (
              id SERIAL PRIMARY KEY,
              first_name VARCHAR(50) NOT NULL,
              last_name VARCHAR(50) NOT NULL,
              email VARCHAR(100) UNIQUE NOT NULL,
              phone VARCHAR(20),
              date_of_birth DATE,
              registration_time TIME,
              last_login TIMESTAMP,
              is_active BOOLEAN DEFAULT TRUE,
              credit_limit DECIMAL(12,2) DEFAULT 0.00,
              total_purchases DECIMAL(15,2) DEFAULT 0.00,
              loyalty_points INTEGER DEFAULT 0,
              customer_data JSONB,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Create products table
      statement.execute(
          """
          CREATE TABLE products (
              id SERIAL PRIMARY KEY,
              category_id INTEGER REFERENCES categories(id),
              name VARCHAR(200) NOT NULL,
              description TEXT,
              price DECIMAL(10,2) NOT NULL,
              cost DECIMAL(10,2) NOT NULL,
              weight REAL,
              dimensions_length DOUBLE PRECISION,
              dimensions_width DOUBLE PRECISION,
              dimensions_height DOUBLE PRECISION,
              stock_quantity INTEGER DEFAULT 0,
              min_stock_level SMALLINT DEFAULT 0,
              barcode BIGINT UNIQUE,
              is_active BOOLEAN DEFAULT TRUE,
              tags TEXT[],
              metadata JSONB,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Create transactions table
      statement.execute(
          """
          CREATE TABLE transactions (
              id SERIAL PRIMARY KEY,
              customer_id INTEGER REFERENCES customers(id),
              product_id INTEGER REFERENCES products(id),
              quantity INTEGER NOT NULL,
              unit_price DECIMAL(10,2) NOT NULL,
              total_amount DECIMAL(12,2) NOT NULL,
              transaction_date DATE NOT NULL,
              transaction_time TIME NOT NULL,
              transaction_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              discount_percent REAL DEFAULT 0.0,
              tax_amount DECIMAL(8,2) DEFAULT 0.00,
              notes TEXT,
              transaction_metadata JSONB,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Create financial_records table for advanced numeric operations
      statement.execute(
          """
          CREATE TABLE financial_records (
              id SERIAL PRIMARY KEY,
              account_number VARCHAR(20) NOT NULL,
              balance DECIMAL(18,4) NOT NULL,
              interest_rate DOUBLE PRECISION,
              monthly_fee REAL,
              last_transaction_amount DECIMAL(15,2),
              record_date DATE NOT NULL,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Create batch_operations table for testing bulk operations
      statement.execute(
          """
          CREATE TABLE batch_operations (
              id SERIAL PRIMARY KEY,
              operation_name VARCHAR(100) NOT NULL,
              batch_id VARCHAR(50) NOT NULL,
              status VARCHAR(20) DEFAULT 'PENDING',
              processed_count INTEGER DEFAULT 0,
              total_count INTEGER NOT NULL,
              started_at TIMESTAMP,
              completed_at TIMESTAMP,
              error_message TEXT,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Create temporal_data table for date/time operations
      statement.execute(
          """
          CREATE TABLE temporal_data (
              id SERIAL PRIMARY KEY,
              event_name VARCHAR(100) NOT NULL,
              event_date DATE NOT NULL,
              event_time TIME NOT NULL,
              event_timestamp TIMESTAMP NOT NULL,
              duration_minutes INTEGER,
              timezone_offset SMALLINT,
              is_recurring BOOLEAN DEFAULT FALSE,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // Insert sample data
      insertSampleData(statement);
    }
  }

  private void insertSampleData(Statement statement) throws SQLException {
    // Insert categories
    statement.execute(
        """
        INSERT INTO categories (name, description) VALUES
        ('Electronics', 'Electronic devices and accessories'),
        ('Books', 'Physical and digital books'),
        ('Clothing', 'Apparel and fashion items'),
        ('Home & Garden', 'Home improvement and gardening supplies')
        """);

    // Insert customers
    statement.execute(
        """
        INSERT INTO customers (first_name, last_name, email, phone, date_of_birth,
                             registration_time, last_login, credit_limit, total_purchases,
                             loyalty_points, customer_data) VALUES
        ('John', 'Doe', 'john.doe@example.com', '+1-555-0101', '1985-03-15',
         '09:30:00', '2024-01-15 14:30:00', 5000.00, 1250.75, 125,
         '{"preferences": {"newsletter": true}, "tier": "gold"}'),
        ('Jane', 'Smith', 'jane.smith@example.com', '+1-555-0102', '1990-07-22',
         '14:15:00', '2024-01-16 10:45:00', 3000.00, 875.50, 88,
         '{"preferences": {"newsletter": false}, "tier": "silver"}'),
        ('Bob', 'Johnson', 'bob.johnson@example.com', '+1-555-0103', '1978-11-08',
         '11:00:00', '2024-01-14 16:20:00', 7500.00, 2100.25, 210,
         '{"preferences": {"newsletter": true}, "tier": "platinum"}')
        """);

    // Insert products
    statement.execute(
        """
        INSERT INTO products (category_id, name, description, price, cost, weight,
                            dimensions_length, dimensions_width, dimensions_height,
                            stock_quantity, min_stock_level, barcode, tags, metadata) VALUES
        (1, 'Laptop Pro', 'High-performance laptop', 1299.99, 899.99, 2.5,
         35.5, 24.5, 2.1, 25, 5, 1234567890123,
         ARRAY['electronics', 'computer', 'portable'],
         '{"warranty": "2 years", "color": "silver"}'),
        (1, 'Wireless Mouse', 'Ergonomic wireless mouse', 45.99, 22.50, 0.15,
         12.0, 6.5, 3.8, 100, 20, 1234567890124,
         ARRAY['electronics', 'computer', 'accessory'],
         '{"warranty": "1 year", "color": "black"}'),
        (2, 'Programming Guide', 'Complete programming reference', 59.99, 25.00, 0.8,
         23.0, 18.0, 3.5, 50, 10, 1234567890125,
         ARRAY['books', 'programming', 'reference'],
         '{"pages": 850, "publisher": "TechBooks"}')
        """);

    // Insert transactions
    statement.execute(
        """
        INSERT INTO transactions (customer_id, product_id, quantity, unit_price, total_amount,
                                transaction_date, transaction_time, discount_percent, tax_amount,
                                notes, transaction_metadata) VALUES
        (1, 1, 1, 1299.99, 1299.99, '2024-01-15', '14:30:00', 0.0, 104.00,
         'Customer paid with credit card', '{"payment_method": "credit_card", "card_type": "visa"}'),
        (1, 2, 2, 45.99, 91.98, '2024-01-15', '14:35:00', 5.0, 7.36,
         'Bulk discount applied', '{"payment_method": "credit_card", "card_type": "visa"}'),
        (2, 2, 1, 45.99, 45.99, '2024-01-16', '10:45:00', 0.0, 3.68,
         'Cash payment', '{"payment_method": "cash"}'),
        (3, 1, 1, 1299.99, 1299.99, '2024-01-14', '16:20:00', 10.0, 104.00,
         'Loyalty discount applied', '{"payment_method": "debit_card", "loyalty_discount": true}')
        """);

    // Insert financial records
    statement.execute(
        """
        INSERT INTO financial_records (account_number, balance, interest_rate, monthly_fee,
                                     last_transaction_amount, record_date) VALUES
        ('ACC001', 15750.5678, 2.5, 15.00, -250.00, '2024-01-15'),
        ('ACC002', 8920.1234, 1.8, 10.00, 1500.75, '2024-01-15'),
        ('ACC003', 25100.9876, 3.2, 25.00, -75.50, '2024-01-15')
        """);

    // Insert temporal data
    statement.execute(
        """
        INSERT INTO temporal_data (event_name, event_date, event_time, event_timestamp,
                                 duration_minutes, timezone_offset, is_recurring) VALUES
        ('Daily Backup', '2024-01-15', '02:00:00', '2024-01-15 02:00:00', 45, -5, true),
        ('Weekly Report', '2024-01-15', '09:00:00', '2024-01-15 09:00:00', 120, -5, true),
        ('System Maintenance', '2024-01-20', '01:00:00', '2024-01-20 01:00:00', 180, -5, false)
        """);
  }

  private void setupComplexOperationRegistry() {
    Map<String, SqlOperationSpec> operations = new HashMap<>();

    // INSERT RETURNING operations
    operations.put(
        "insertCustomerReturning",
        SqlOperationSpec.builder()
            .sql(
                """
                INSERT INTO customers (first_name, last_name, email, phone, date_of_birth,
                                     registration_time, credit_limit, customer_data)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                RETURNING id, first_name, last_name, email, created_at
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("firstName", SqlParameterType.STRING),
                    SqlParameter.in("lastName", SqlParameterType.STRING),
                    SqlParameter.in("email", SqlParameterType.STRING),
                    SqlParameter.in("phone", SqlParameterType.STRING),
                    SqlParameter.in("dateOfBirth", SqlParameterType.DATE),
                    SqlParameter.in("registrationTime", SqlParameterType.TIME),
                    SqlParameter.in("creditLimit", SqlParameterType.DECIMAL),
                    SqlParameter.in("customerData", SqlParameterType.STRING)))
            .build());

    operations.put(
        "insertProductReturning",
        SqlOperationSpec.builder()
            .sql(
                """
                INSERT INTO products (category_id, name, price, cost, weight, stock_quantity, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                RETURNING id, name, price, stock_quantity, created_at
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("categoryId", SqlParameterType.INTEGER),
                    SqlParameter.in("name", SqlParameterType.STRING),
                    SqlParameter.in("price", SqlParameterType.DECIMAL),
                    SqlParameter.in("cost", SqlParameterType.DECIMAL),
                    SqlParameter.in("weight", SqlParameterType.FLOAT),
                    SqlParameter.in("stockQuantity", SqlParameterType.INTEGER),
                    SqlParameter.in("metadata", SqlParameterType.STRING)))
            .build());

    // UNION operations
    operations.put(
        "getUnionResults",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT 'customer' as source, id, first_name as name, email, created_at
                FROM customers
                WHERE credit_limit > ?
                UNION ALL
                SELECT 'product' as source, id, name, NULL as email, created_at
                FROM products
                WHERE price > ?
                ORDER BY created_at DESC
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("minCreditLimit", SqlParameterType.DECIMAL),
                    SqlParameter.in("minPrice", SqlParameterType.DECIMAL)))
            .build());

    // Complex JOIN with multiple tables
    operations.put(
        "getComplexTransactionReport",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT
                    c.first_name || ' ' || c.last_name as customer_name,
                    cat.name as category_name,
                    p.name as product_name,
                    t.quantity,
                    t.unit_price,
                    t.total_amount,
                    t.discount_percent,
                    t.tax_amount,
                    (t.total_amount + t.tax_amount) as final_amount,
                    t.transaction_date,
                    t.transaction_time,
                    c.loyalty_points,
                    p.stock_quantity
                FROM transactions t
                INNER JOIN customers c ON t.customer_id = c.id
                INNER JOIN products p ON t.product_id = p.id
                INNER JOIN categories cat ON p.category_id = cat.id
                WHERE t.transaction_date BETWEEN ? AND ?
                ORDER BY t.transaction_timestamp DESC
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("startDate", SqlParameterType.DATE),
                    SqlParameter.in("endDate", SqlParameterType.DATE)))
            .build());

    // Aggregate functions with CASE statements
    operations.put(
        "getAdvancedCustomerAnalytics",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT
                    c.id,
                    c.first_name || ' ' || c.last_name as customer_name,
                    COUNT(t.id) as total_transactions,
                    SUM(t.total_amount) as total_spent,
                    AVG(t.total_amount) as avg_transaction_amount,
                    MIN(t.transaction_date) as first_transaction_date,
                    MAX(t.transaction_date) as last_transaction_date,
                    SUM(CASE WHEN t.discount_percent > 0 THEN 1 ELSE 0 END) as discounted_transactions,
                    CASE
                        WHEN SUM(t.total_amount) > 2000 THEN 'HIGH_VALUE'
                        WHEN SUM(t.total_amount) > 500 THEN 'MEDIUM_VALUE'
                        ELSE 'LOW_VALUE'
                    END as customer_tier,
                    CASE
                        WHEN c.loyalty_points > 200 THEN 'PLATINUM'
                        WHEN c.loyalty_points > 100 THEN 'GOLD'
                        WHEN c.loyalty_points > 50 THEN 'SILVER'
                        ELSE 'BRONZE'
                    END as loyalty_level
                FROM customers c
                LEFT JOIN transactions t ON c.id = t.customer_id
                GROUP BY c.id, c.first_name, c.last_name, c.loyalty_points
                HAVING COUNT(t.id) >= ?
                ORDER BY total_spent DESC NULLS LAST
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(List.of(SqlParameter.in("minTransactionCount", SqlParameterType.INTEGER)))
            .build());

    // Window functions
    operations.put(
        "getTransactionRankings",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT
                    t.id,
                    c.first_name || ' ' || c.last_name as customer_name,
                    p.name as product_name,
                    t.total_amount,
                    t.transaction_date,
                    ROW_NUMBER() OVER (ORDER BY t.total_amount DESC) as amount_rank,
                    RANK() OVER (PARTITION BY t.customer_id ORDER BY t.total_amount DESC) as customer_rank,
                    LAG(t.total_amount) OVER (PARTITION BY t.customer_id ORDER BY t.transaction_date) as previous_amount,
                    LEAD(t.total_amount) OVER (PARTITION BY t.customer_id ORDER BY t.transaction_date) as next_amount,
                    SUM(t.total_amount) OVER (PARTITION BY t.customer_id) as customer_total,
                    AVG(t.total_amount) OVER (PARTITION BY t.customer_id) as customer_avg
                FROM transactions t
                INNER JOIN customers c ON t.customer_id = c.id
                INNER JOIN products p ON t.product_id = p.id
                ORDER BY t.total_amount DESC
                """)
            .operationType(SqlOperationType.QUERY)
            .build());

    // Subqueries and EXISTS
    operations.put(
        "getCustomersWithHighValueTransactions",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT
                    c.id,
                    c.first_name,
                    c.last_name,
                    c.email,
                    c.total_purchases,
                    (SELECT COUNT(*) FROM transactions t WHERE t.customer_id = c.id) as transaction_count,
                    (SELECT MAX(total_amount) FROM transactions t WHERE t.customer_id = c.id) as max_transaction
                FROM customers c
                WHERE EXISTS (
                    SELECT 1 FROM transactions t
                    WHERE t.customer_id = c.id
                    AND t.total_amount > ?
                )
                AND c.credit_limit > (
                    SELECT AVG(credit_limit) FROM customers
                    WHERE is_active = true
                )
                ORDER BY c.total_purchases DESC
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(List.of(SqlParameter.in("minTransactionAmount", SqlParameterType.DECIMAL)))
            .build());

    // UPDATE RETURNING
    operations.put(
        "updateProductStockReturning",
        SqlOperationSpec.builder()
            .sql(
                """
                UPDATE products
                SET stock_quantity = stock_quantity + ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                RETURNING id, name, stock_quantity, updated_at
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("quantityChange", SqlParameterType.INTEGER),
                    SqlParameter.in("productId", SqlParameterType.INTEGER)))
            .build());

    // CTE (Common Table Expressions)
    operations.put(
        "getCustomerAnalyticsWithCTE",
        SqlOperationSpec.builder()
            .sql(
                """
                WITH customer_stats AS (
                    SELECT
                        customer_id,
                        COUNT(*) as transaction_count,
                        SUM(total_amount) as total_spent,
                        AVG(total_amount) as avg_spent
                    FROM transactions
                    GROUP BY customer_id
                ),
                customer_rankings AS (
                    SELECT
                        customer_id,
                        total_spent,
                        RANK() OVER (ORDER BY total_spent DESC) as spending_rank
                    FROM customer_stats
                )
                SELECT
                    c.id,
                    c.first_name || ' ' || c.last_name as customer_name,
                    c.email,
                    cs.transaction_count,
                    cs.total_spent,
                    cs.avg_spent,
                    cr.spending_rank,
                    CASE
                        WHEN cr.spending_rank <= 3 THEN 'TOP_SPENDER'
                        WHEN cr.spending_rank <= 10 THEN 'HIGH_SPENDER'
                        ELSE 'REGULAR_SPENDER'
                    END as spender_category
                FROM customers c
                INNER JOIN customer_stats cs ON c.id = cs.customer_id
                INNER JOIN customer_rankings cr ON c.id = cr.customer_id
                WHERE cs.total_spent > ?
                ORDER BY cr.spending_rank
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(List.of(SqlParameter.in("minTotalSpent", SqlParameterType.DECIMAL)))
            .build());

    // Array operations
    operations.put(
        "getProductsByTags",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT
                    p.id,
                    p.name,
                    p.price,
                    p.tags,
                    array_length(p.tags, 1) as tag_count,
                    p.tags && ? as has_required_tags
                FROM products p
                WHERE p.tags && ?
                ORDER BY array_length(p.tags, 1) DESC, p.price ASC
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("searchTags1", SqlParameterType.ARRAY),
                    SqlParameter.in("searchTags2", SqlParameterType.ARRAY)))
            .build());

    // JSON operations
    operations.put(
        "getCustomersByJsonPreferences",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT
                    c.id,
                    c.first_name,
                    c.last_name,
                    c.email,
                    c.customer_data->>'tier' as customer_tier,
                    (c.customer_data->'preferences'->>'newsletter')::boolean as newsletter_preference,
                    c.customer_data
                FROM customers c
                WHERE c.customer_data->>'tier' = ?
                   OR (c.customer_data->'preferences'->>'newsletter')::boolean = ?
                ORDER BY c.customer_data->>'tier', c.last_name
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("tierFilter", SqlParameterType.STRING),
                    SqlParameter.in("newsletterPreference", SqlParameterType.BOOLEAN)))
            .build());

    // Date/Time operations
    operations.put(
        "getTemporalAnalytics",
        SqlOperationSpec.builder()
            .sql(
                """
                SELECT
                    td.id,
                    td.event_name,
                    td.event_date,
                    td.event_time,
                    td.event_timestamp,
                    EXTRACT(DOW FROM td.event_date) as day_of_week,
                    EXTRACT(HOUR FROM td.event_time) as hour_of_day,
                    DATE_PART('epoch', td.event_timestamp) as epoch_seconds,
                    td.event_date + INTERVAL '30 days' as future_date,
                    AGE(CURRENT_DATE, td.event_date) as age_from_today,
                    CASE
                        WHEN td.event_time BETWEEN ? AND ? THEN 'BUSINESS_HOURS'
                        ELSE 'OFF_HOURS'
                    END as time_category
                FROM temporal_data td
                WHERE td.event_date BETWEEN ? AND ?
                ORDER BY td.event_timestamp DESC
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("businessStartTime", SqlParameterType.TIME),
                    SqlParameter.in("businessEndTime", SqlParameterType.TIME),
                    SqlParameter.in("startDate", SqlParameterType.DATE),
                    SqlParameter.in("endDate", SqlParameterType.DATE)))
            .build());

    // Batch insert simulation
    operations.put(
        "insertBatchOperation",
        SqlOperationSpec.builder()
            .sql(
                """
                INSERT INTO batch_operations (operation_name, batch_id, total_count, started_at)
                VALUES (?, ?, ?, ?)
                RETURNING id, operation_name, batch_id, status, created_at
                """)
            .operationType(SqlOperationType.QUERY)
            .parameters(
                List.of(
                    SqlParameter.in("operationName", SqlParameterType.STRING),
                    SqlParameter.in("batchId", SqlParameterType.STRING),
                    SqlParameter.in("totalCount", SqlParameterType.INTEGER),
                    SqlParameter.in("startedAt", SqlParameterType.TIMESTAMP)))
            .build());

    registry = new SqlOperationRegistry(operations);
  }

  // INSERT RETURNING Tests
  @Test
  @DisplayName("Should execute INSERT RETURNING and return generated values")
  void shouldExecuteInsertReturningAndReturnGeneratedValues() throws SQLException {
    LocalDate birthDate = LocalDate.of(1992, 5, 18);
    LocalTime regTime = LocalTime.of(15, 30, 0);

    SqlOperationResult result =
        operationManager.execute(
            "insertCustomerReturning",
            new Object[] {
              "Alice",
              "Cooper",
              "alice.cooper@example.com",
              "+1-555-0200",
              Date.valueOf(birthDate),
              Time.valueOf(regTime),
              new BigDecimal("4500.00"),
              "{\"preferences\": {\"newsletter\": true}, \"tier\": \"silver\"}"
            });

    assertNotNull(result);
    assertTrue(result.isSuccess());
    // For INSERT RETURNING, affected rows might be 0 since it's a query operation
    assertEquals(0, result.getAffectedRows());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(2, resultSet.size()); // Header + 1 data row

    // Check header
    List<Object> headers = resultSet.get(0);
    assertEquals(List.of("id", "first_name", "last_name", "email", "created_at"), headers);

    // Check returned data
    List<Object> customerData = resultSet.get(1);
    assertNotNull(customerData.get(0)); // id should be generated
    assertEquals("Alice", customerData.get(1)); // first_name
    assertEquals("Cooper", customerData.get(2)); // last_name
    assertEquals("alice.cooper@example.com", customerData.get(3)); // email
    assertNotNull(customerData.get(4)); // created_at should be set
  }

  @Test
  @DisplayName("Should execute INSERT RETURNING for products with various data types")
  void shouldExecuteInsertReturningForProductsWithVariousDataTypes() throws SQLException {
    SqlOperationResult result =
        operationManager.execute(
            "insertProductReturning",
            new Object[] {
              1,
              "Smart Watch",
              new BigDecimal("299.99"),
              new BigDecimal("180.00"),
              0.25f,
              15,
              "{\"warranty\": \"1 year\", \"waterproof\": true}"
            });

    assertNotNull(result);
    assertTrue(result.isSuccess());
    // For INSERT RETURNING, affected rows might be 0 since it's a query operation
    assertEquals(0, result.getAffectedRows());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    List<Object> productData = resultSet.get(1);

    assertNotNull(productData.get(0)); // id
    assertEquals("Smart Watch", productData.get(1)); // name
    assertTrue(productData.get(2) instanceof BigDecimal); // price
    assertEquals(15, productData.get(3)); // stock_quantity
    assertNotNull(productData.get(4)); // created_at
  }

  // UNION Tests
  @Test
  @DisplayName("Should execute UNION query combining customers and products")
  void shouldExecuteUnionQueryCombiningCustomersAndProducts() throws SQLException {
    SqlOperationResult result =
        operationManager.execute(
            "getUnionResults", new Object[] {new BigDecimal("4000.00"), new BigDecimal("100.00")});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertTrue(resultSet.size() > 1); // Should have header + data rows

    // Check header
    List<Object> headers = resultSet.get(0);
    assertEquals(List.of("source", "id", "name", "email", "created_at"), headers);

    // Verify data contains both customers and products
    boolean hasCustomer = false;
    boolean hasProduct = false;
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      String source = (String) row.get(0);
      if ("customer".equals(source)) {
        hasCustomer = true;
        assertNotNull(row.get(3)); // customers should have email
      } else if ("product".equals(source)) {
        hasProduct = true;
        assertNull(row.get(3)); // products should have null email
      }
    }

    assertTrue(
        hasCustomer || hasProduct, "Should contain either customers or products based on criteria");
  }

  // Complex JOIN Tests
  @Test
  @DisplayName("Should execute complex JOIN query with multiple tables")
  void shouldExecuteComplexJoinQueryWithMultipleTables() throws SQLException {
    LocalDate startDate = LocalDate.of(2024, 1, 1);
    LocalDate endDate = LocalDate.of(2024, 12, 31);

    SqlOperationResult result =
        operationManager.execute(
            "getComplexTransactionReport",
            new Object[] {Date.valueOf(startDate), Date.valueOf(endDate)});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertTrue(resultSet.size() > 1); // Should have data

    // Check header contains all expected columns
    List<Object> headers = resultSet.get(0);
    assertTrue(headers.contains("customer_name"));
    assertTrue(headers.contains("category_name"));
    assertTrue(headers.contains("product_name"));
    assertTrue(headers.contains("final_amount"));

    // Verify data integrity
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      assertNotNull(row.get(0)); // customer_name
      assertNotNull(row.get(1)); // category_name
      assertNotNull(row.get(2)); // product_name
      assertTrue(row.get(8) instanceof BigDecimal); // final_amount
    }
  }

  // Aggregate and CASE Tests
  @Test
  @DisplayName("Should execute advanced analytics with aggregates and CASE statements")
  void shouldExecuteAdvancedAnalyticsWithAggregatesAndCaseStatements() throws SQLException {
    SqlOperationResult result =
        operationManager.execute(
            "getAdvancedCustomerAnalytics", new Object[] {0}); // minimum transaction count = 0

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertTrue(resultSet.size() > 1);

    // Check for expected computed columns
    List<Object> headers = resultSet.get(0);
    assertTrue(headers.contains("customer_tier"));
    assertTrue(headers.contains("loyalty_level"));
    assertTrue(headers.contains("total_transactions"));
    assertTrue(headers.contains("avg_transaction_amount"));

    // Verify CASE statement results
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      String customerTier = (String) row.get(8); // customer_tier
      String loyaltyLevel = (String) row.get(9); // loyalty_level

      assertTrue(List.of("HIGH_VALUE", "MEDIUM_VALUE", "LOW_VALUE").contains(customerTier));
      assertTrue(List.of("PLATINUM", "GOLD", "SILVER", "BRONZE").contains(loyaltyLevel));
    }
  }

  // Window Functions Tests
  @Test
  @DisplayName("Should execute window functions for transaction rankings")
  void shouldExecuteWindowFunctionsForTransactionRankings() throws SQLException {
    SqlOperationResult result = operationManager.execute("getTransactionRankings", new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertTrue(resultSet.size() > 1);

    // Check for window function columns
    List<Object> headers = resultSet.get(0);
    assertTrue(headers.contains("amount_rank"));
    assertTrue(headers.contains("customer_rank"));
    assertTrue(headers.contains("customer_total"));
    assertTrue(headers.contains("customer_avg"));

    // Verify ranking is working (first row should have rank 1)
    if (resultSet.size() > 1) {
      List<Object> firstRow = resultSet.get(1);
      assertEquals(1L, firstRow.get(5)); // amount_rank should be 1 for highest amount
    }
  }

  // Subquery and EXISTS Tests
  @Test
  @DisplayName("Should execute subquery with EXISTS clause")
  void shouldExecuteSubqueryWithExistsClause() throws SQLException {
    SqlOperationResult result =
        operationManager.execute(
            "getCustomersWithHighValueTransactions", new Object[] {new BigDecimal("1000.00")});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);

    // Check subquery columns are included
    List<Object> headers = resultSet.get(0);
    assertTrue(headers.contains("transaction_count"));
    assertTrue(headers.contains("max_transaction"));

    // All returned customers should have high-value transactions
    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      BigDecimal maxTransaction = (BigDecimal) row.get(6);
      assertTrue(maxTransaction.compareTo(new BigDecimal("1000.00")) >= 0);
    }
  }

  // UPDATE RETURNING Tests
  @Test
  @DisplayName("Should execute UPDATE RETURNING for stock management")
  void shouldExecuteUpdateReturningForStockManagement() throws SQLException {
    SqlOperationResult result =
        operationManager.execute(
            "updateProductStockReturning", new Object[] {50, 1}); // Add 50 units to product 1

    assertNotNull(result);
    assertTrue(result.isSuccess());
    // For UPDATE RETURNING, affected rows might be 0 since it's a query operation
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(2, resultSet.size()); // Header + 1 data row

    List<Object> productData = resultSet.get(1);
    assertEquals(1, productData.get(0)); // product id
    assertNotNull(productData.get(1)); // product name
    assertEquals(75, productData.get(2)); // updated stock (original 25 + 50)
    assertNotNull(productData.get(3)); // updated_at timestamp
  }

  // CTE Tests
  @Test
  @DisplayName("Should execute CTE query for customer analytics")
  void shouldExecuteCteQueryForCustomerAnalytics() throws SQLException {
    SqlOperationResult result =
        operationManager.execute(
            "getCustomerAnalyticsWithCTE", new Object[] {new BigDecimal("100.00")});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);

    if (resultSet.size() > 1) {
      List<Object> headers = resultSet.get(0);
      assertTrue(headers.contains("spending_rank"));
      assertTrue(headers.contains("spender_category"));

      // Verify ranking order (should be ordered by spending_rank)
      for (int i = 1; i < resultSet.size() - 1; i++) {
        List<Object> currentRow = resultSet.get(i);
        List<Object> nextRow = resultSet.get(i + 1);

        Long currentRank = (Long) currentRow.get(6); // spending_rank
        Long nextRank = (Long) nextRow.get(6);

        assertTrue(currentRank <= nextRank, "Results should be ordered by spending rank");
      }
    }
  }

  // Date/Time Operations Tests
  @Test
  @DisplayName("Should execute temporal analytics with date/time functions")
  void shouldExecuteTemporalAnalyticsWithDateTimeFunctions() throws SQLException {
    LocalTime businessStart = LocalTime.of(9, 0);
    LocalTime businessEnd = LocalTime.of(17, 0);
    LocalDate startDate = LocalDate.of(2024, 1, 1);
    LocalDate endDate = LocalDate.of(2024, 12, 31);

    SqlOperationResult result =
        operationManager.execute(
            "getTemporalAnalytics",
            new Object[] {
              Time.valueOf(businessStart),
              Time.valueOf(businessEnd),
              Date.valueOf(startDate),
              Date.valueOf(endDate)
            });

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);

    if (resultSet.size() > 1) {
      List<Object> headers = resultSet.get(0);
      assertTrue(headers.contains("day_of_week"));
      assertTrue(headers.contains("hour_of_day"));
      assertTrue(headers.contains("epoch_seconds"));
      assertTrue(headers.contains("time_category"));

      // Verify computed values
      for (int i = 1; i < resultSet.size(); i++) {
        List<Object> row = resultSet.get(i);
        Number dayOfWeekNum = (Number) row.get(5);
        Number hourOfDayNum = (Number) row.get(6);
        String timeCategory = (String) row.get(10);

        double dayOfWeek = dayOfWeekNum.doubleValue();
        double hourOfDay = hourOfDayNum.doubleValue();

        assertTrue(dayOfWeek >= 0 && dayOfWeek <= 6); // 0=Sunday, 6=Saturday
        assertTrue(hourOfDay >= 0 && hourOfDay <= 23);
        assertTrue(List.of("BUSINESS_HOURS", "OFF_HOURS").contains(timeCategory));
      }
    }
  }

  // Batch Operations Tests
  @Test
  @DisplayName("Should execute batch operation insert with timestamp")
  void shouldExecuteBatchOperationInsertWithTimestamp() throws SQLException {
    LocalDateTime now = LocalDateTime.now();
    Timestamp startTime = Timestamp.valueOf(now);

    SqlOperationResult result =
        operationManager.execute(
            "insertBatchOperation", new Object[] {"DATA_MIGRATION", "BATCH_001", 1000, startTime});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    // For INSERT RETURNING, affected rows might be 0 since it's a query operation
    assertEquals(1, result.getResultSets().size());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(2, resultSet.size());

    List<Object> batchData = resultSet.get(1);
    assertNotNull(batchData.get(0)); // id
    assertEquals("DATA_MIGRATION", batchData.get(1)); // operation_name
    assertEquals("BATCH_001", batchData.get(2)); // batch_id
    assertEquals("PENDING", batchData.get(3)); // status
    assertNotNull(batchData.get(4)); // created_at
  }

  // Error Handling for Complex Queries
  @Test
  @DisplayName("Should handle errors in complex queries gracefully")
  void shouldHandleErrorsInComplexQueriesGracefully() throws SQLException {
    // Test with invalid date range (end before start)
    LocalDate endDate = LocalDate.of(2024, 1, 1);
    LocalDate startDate = LocalDate.of(2024, 12, 31);

    // This should still work but return no results
    SqlOperationResult result =
        operationManager.execute(
            "getComplexTransactionReport",
            new Object[] {Date.valueOf(startDate), Date.valueOf(endDate)});

    assertNotNull(result);
    assertTrue(result.isSuccess());

    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(1, resultSet.size()); // Only header, no data
  }

  @Test
  @DisplayName("Should handle NULL values in complex calculations")
  void shouldHandleNullValuesInComplexCalculations() throws SQLException {
    // Insert a customer without transactions to test NULL handling
    operationManager.execute(
        "insertCustomerReturning",
        new Object[] {
          "No",
          "Transactions",
          "no.transactions@example.com",
          null,
          Date.valueOf(LocalDate.of(1995, 1, 1)),
          Time.valueOf(LocalTime.of(12, 0)),
          new BigDecimal("1000.00"),
          "{\"tier\": \"bronze\"}"
        });

    SqlOperationResult result =
        operationManager.execute("getAdvancedCustomerAnalytics", new Object[] {0});

    assertNotNull(result);
    assertTrue(result.isSuccess());

    // Should include customers with no transactions (showing NULL handling)
    List<List<Object>> resultSet = result.getResultSets().get(0);
    boolean foundCustomerWithNoTransactions = false;

    for (int i = 1; i < resultSet.size(); i++) {
      List<Object> row = resultSet.get(i);
      if ("No Transactions".equals(row.get(1))) { // customer_name
        foundCustomerWithNoTransactions = true;
        assertEquals(0L, row.get(2)); // total_transactions should be 0
        // total_spent might be null or 0
        assertTrue(
            row.get(3) == null || BigDecimal.ZERO.equals(row.get(3)),
            "total_spent should be null or 0");
        break;
      }
    }

    assertTrue(foundCustomerWithNoTransactions, "Should handle customers with no transactions");
  }
}
