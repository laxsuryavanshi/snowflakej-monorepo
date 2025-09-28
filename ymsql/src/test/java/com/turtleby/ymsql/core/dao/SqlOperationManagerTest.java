package com.turtleby.ymsql.core.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.turtleby.ymsql.core.model.SqlOperationResult;
import com.turtleby.ymsql.core.model.SqlOperationSpec;
import com.turtleby.ymsql.core.model.SqlOperationSpec.SqlOperationType;
import com.turtleby.ymsql.core.model.SqlParameter;
import com.turtleby.ymsql.core.model.SqlParameterMode;
import com.turtleby.ymsql.core.model.SqlParameterType;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqlOperationManager Tests")
class SqlOperationManagerTest {

  @Mock private SqlOperationRegistry mockRegistry;
  @Mock private DataSource mockDataSource;
  @Mock private Connection mockConnection;
  @Mock private PreparedStatement mockPreparedStatement;
  @Mock private CallableStatement mockCallableStatement;
  @Mock private ResultSet mockResultSet;
  @Mock private ResultSetMetaData mockMetaData;

  private SqlOperationManager operationManager;

  @BeforeEach
  void setUp() {
    operationManager = new SqlOperationManager(mockRegistry, mockDataSource);
  }

  @Test
  @DisplayName("Constructor should validate required dependencies")
  void constructorShouldValidateRequiredDependencies() {
    // Test null registry
    assertThrows(
        NullPointerException.class,
        () -> new SqlOperationManager(null, mockDataSource),
        "Should throw exception for null registry");

    // Test null data source
    assertThrows(
        NullPointerException.class,
        () -> new SqlOperationManager(mockRegistry, null),
        "Should throw exception for null data source");

    // Test valid construction
    assertDoesNotThrow(
        () -> new SqlOperationManager(mockRegistry, mockDataSource),
        "Should create manager with valid dependencies");
  }

  @Test
  @DisplayName("Execute should validate operation name")
  void executeShouldValidateOperationName() throws SQLException {
    // Test null operation name
    assertThrows(
        IllegalArgumentException.class,
        () -> operationManager.execute(null, new Object[0]),
        "Should throw exception for null operation name");

    // Test empty operation name
    assertThrows(
        IllegalArgumentException.class,
        () -> operationManager.execute("", new Object[0]),
        "Should throw exception for empty operation name");

    // Test blank operation name
    assertThrows(
        IllegalArgumentException.class,
        () -> operationManager.execute("   ", new Object[0]),
        "Should throw exception for blank operation name");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", " \t\n", "\t", "\n"})
  void executeShouldValidateOperationName(String operationName) throws SQLException {
    assertThrows(
        IllegalArgumentException.class,
        () -> operationManager.execute(operationName, new Object[0]),
        "Should throw exception for invalid operation name: '" + operationName + "'");
  }

  @Test
  @DisplayName("Execute should handle registry operation retrieval failure")
  void executeShouldHandleRegistryOperationRetrievalFailure() throws SQLException {
    String operationName = "nonExistentOperation";

    when(mockRegistry.getOperationSpec(operationName))
        .thenThrow(new IllegalArgumentException("Operation not found"));

    assertThrows(
        IllegalArgumentException.class,
        () -> operationManager.execute(operationName, new Object[0]),
        "Should propagate registry exceptions");

    verify(mockRegistry).getOperationSpec(operationName);
  }

  @Test
  @DisplayName("Execute should handle parameter validation failure")
  void executeShouldHandleParameterValidationFailure() throws SQLException {
    String operationName = "testOperation";
    SqlOperationSpec spec =
        createQuerySpec(
            "SELECT * FROM test WHERE id = ?",
            List.of(createInParameter("id", SqlParameterType.INTEGER)));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);

    // Missing required parameter
    assertThrows(
        IllegalArgumentException.class,
        () -> operationManager.execute(operationName, new Object[0]),
        "Should throw exception for missing parameters");

    verify(mockRegistry).getOperationSpec(operationName);
  }

  @Test
  @DisplayName("Execute query should handle successful execution")
  void executeQueryShouldHandleSuccessfulExecution() throws SQLException {
    String operationName = "getUser";
    String sql = "SELECT id, name FROM users WHERE id = ?";
    SqlOperationSpec spec =
        createQuerySpec(sql, List.of(createInParameter("id", SqlParameterType.INTEGER)));

    setupSuccessfulQueryExecution(operationName, spec, sql);

    SqlOperationResult result = operationManager.execute(operationName, new Object[] {123});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(sql, result.getExecutedSql());
    assertEquals(1, result.getResultSets().size());
    assertTrue(result.getExecutionTimeMs() >= 0);

    verify(mockRegistry).getOperationSpec(operationName);
    verify(mockDataSource).getConnection();
    verify(mockConnection).prepareStatement(sql);
    verify(mockPreparedStatement).setInt(1, 123);
    verify(mockPreparedStatement).execute();
    verify(mockPreparedStatement).getResultSet();
  }

  @Test
  @DisplayName("Execute query should handle multiple result sets")
  void executeQueryShouldHandleMultipleResultSets() throws SQLException {
    String operationName = "getMultipleData";
    String sql = "SELECT * FROM table1; SELECT * FROM table2;";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(true);

    // First result set
    ResultSet mockResultSet1 = mock(ResultSet.class);
    ResultSetMetaData mockMetaData1 = mock(ResultSetMetaData.class);

    // Second result set
    ResultSet mockResultSet2 = mock(ResultSet.class);
    ResultSetMetaData mockMetaData2 = mock(ResultSetMetaData.class);

    when(mockPreparedStatement.getResultSet())
        .thenReturn(mockResultSet1)
        .thenReturn(mockResultSet2);

    when(mockResultSet1.getMetaData()).thenReturn(mockMetaData1);
    when(mockMetaData1.getColumnCount()).thenReturn(1);
    when(mockMetaData1.getColumnLabel(1)).thenReturn("col1");
    when(mockResultSet1.next()).thenReturn(false);

    when(mockResultSet2.getMetaData()).thenReturn(mockMetaData2);
    when(mockMetaData2.getColumnCount()).thenReturn(1);
    when(mockMetaData2.getColumnLabel(1)).thenReturn("col2");
    when(mockResultSet2.next()).thenReturn(false);

    // Configure the multiple result sets behavior
    when(mockPreparedStatement.getMoreResults()).thenReturn(true, false);
    when(mockPreparedStatement.getUpdateCount()).thenReturn(-1);

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(2, result.getResultSets().size());

    verify(mockConnection).prepareStatement(sql);
    verify(mockPreparedStatement).execute();
    verify(mockPreparedStatement, times(2)).getResultSet();
    verify(mockPreparedStatement, times(2)).getMoreResults();
    verify(mockPreparedStatement).getUpdateCount();
  }

  @Test
  @DisplayName("Execute query should handle update count operations")
  void executeQueryShouldHandleUpdateCountOperations() throws SQLException {
    String operationName = "updateUser";
    String sql = "UPDATE users SET name = ? WHERE id = ?";
    SqlOperationSpec spec =
        createQuerySpec(
            sql,
            List.of(
                createInParameter("name", SqlParameterType.STRING),
                createInParameter("id", SqlParameterType.INTEGER)));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(false);
    when(mockPreparedStatement.getUpdateCount()).thenReturn(1, -1);
    when(mockPreparedStatement.getMoreResults()).thenReturn(false);

    SqlOperationResult result =
        operationManager.execute(operationName, new Object[] {"John Doe", 123});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getAffectedRows());

    verify(mockConnection).prepareStatement(sql);
    verify(mockPreparedStatement).setString(1, "John Doe");
    verify(mockPreparedStatement).setInt(2, 123);
    verify(mockPreparedStatement).execute();
    verify(mockPreparedStatement, times(2)).getUpdateCount();
    verify(mockPreparedStatement, never()).getResultSet();
    verify(mockPreparedStatement).getMoreResults();
  }

  @Test
  @DisplayName("Execute procedure should handle successful execution")
  void executeProcedureShouldHandleSuccessfulExecution() throws SQLException {
    String operationName = "getUserProcedure";
    String sql = "{call get_user_by_id(?, ?)}";
    SqlOperationSpec spec =
        createProcedureSpec(
            sql,
            List.of(
                createInParameter("id", SqlParameterType.INTEGER),
                createOutParameter("name", SqlParameterType.STRING)));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);
    when(mockCallableStatement.getObject(2)).thenReturn("John Doe");

    SqlOperationResult result = operationManager.execute(operationName, new Object[] {123});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(sql, result.getExecutedSql());
    assertEquals(1, result.getOutputParameters().size());
    assertTrue(result.getOutputParameters().containsKey("name"));
    assertEquals("John Doe", result.getOutputParameters().get("name"));

    verify(mockConnection).prepareCall(sql);
    verify(mockCallableStatement).setInt(1, 123);
    verify(mockCallableStatement).registerOutParameter(2, Types.VARCHAR);
    verify(mockCallableStatement).execute();
    verify(mockCallableStatement).getObject(2);
  }

  @Test
  @DisplayName("Execute procedure should handle REFCURSOR parameters")
  void executeProcedureShouldHandleRefCursorParameters() throws SQLException {
    String operationName = "getUsersCursor";
    String sql = "{call get_all_users(?)}";
    SqlOperationSpec spec =
        createProcedureSpec(sql, List.of(createRefCursorParameter("users_cursor")));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);

    // Mock REFCURSOR result set
    ResultSet mockCursorResultSet = mock(ResultSet.class);
    ResultSetMetaData mockCursorMetaData = mock(ResultSetMetaData.class);
    when(mockCallableStatement.getObject(1)).thenReturn(mockCursorResultSet);
    when(mockCursorResultSet.getMetaData()).thenReturn(mockCursorMetaData);
    when(mockCursorMetaData.getColumnCount()).thenReturn(2);
    when(mockCursorMetaData.getColumnLabel(1)).thenReturn("id");
    when(mockCursorMetaData.getColumnLabel(2)).thenReturn("name");
    when(mockCursorResultSet.next()).thenReturn(true, false);
    when(mockCursorResultSet.getObject(1)).thenReturn(1);
    when(mockCursorResultSet.getObject(2)).thenReturn("John");

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getOutputParameters().size());
    assertTrue(result.getOutputParameters().containsKey("users_cursor"));

    @SuppressWarnings("unchecked")
    List<List<Object>> cursorData =
        (List<List<Object>>) result.getOutputParameters().get("users_cursor");
    assertEquals(2, cursorData.size()); // Header + 1 data row

    verify(mockConnection).setAutoCommit(false);
    verify(mockConnection).commit();
    verify(mockConnection).setAutoCommit(true);
    verify(mockCallableStatement).registerOutParameter(1, Types.REF_CURSOR);
    verify(mockCallableStatement).execute();
    verify(mockCallableStatement).getObject(1);
    verify(mockCursorResultSet, times(2)).next();
    verify(mockCursorResultSet).getObject(1);
    verify(mockCursorResultSet).getObject(2);
  }

  @Test
  @DisplayName("Execute procedure should handle INOUT parameters")
  void executeProcedureShouldHandleInOutParameters() throws SQLException {
    String operationName = "processValue";
    String sql = "{call process_value(?)}";
    SqlOperationSpec spec =
        createProcedureSpec(sql, List.of(createInOutParameter("value", SqlParameterType.STRING)));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);
    when(mockCallableStatement.getObject(1)).thenReturn("processed_value");

    SqlOperationResult result =
        operationManager.execute(operationName, new Object[] {"input_value"});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getOutputParameters().size());
    assertTrue(result.getOutputParameters().containsKey("value"));
    assertEquals("processed_value", result.getOutputParameters().get("value"));

    verify(mockCallableStatement).setString(1, "input_value");
    verify(mockCallableStatement).registerOutParameter(1, Types.VARCHAR);
  }

  @Test
  @DisplayName("Execute should handle SQLException during connection acquisition")
  void executeShouldHandleSQLExceptionDuringConnectionAcquisition() throws SQLException {
    String operationName = "testOperation";
    SqlOperationSpec spec = createQuerySpec("SELECT 1", Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

    assertThrows(
        SQLException.class,
        () -> operationManager.execute(operationName, new Object[0]),
        "Should propagate SQLException from connection acquisition");
  }

  @Test
  @DisplayName("Execute should handle SQLException during statement preparation")
  void executeShouldHandleSQLExceptionDuringStatementPreparation() throws SQLException {
    String operationName = "testOperation";
    String sql = "INVALID SQL";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenThrow(new SQLException("SQL syntax error"));

    assertThrows(
        SQLException.class,
        () -> operationManager.execute(operationName, new Object[0]),
        "Should propagate SQLException from statement preparation");
  }

  @Test
  @DisplayName("Execute should handle SQLException during statement execution")
  void executeShouldHandleSQLExceptionDuringStatementExecution() throws SQLException {
    String operationName = "testOperation";
    String sql = "SELECT * FROM non_existent_table";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenThrow(new SQLException("Table does not exist"));

    assertThrows(
        SQLException.class,
        () -> operationManager.execute(operationName, new Object[0]),
        "Should propagate SQLException from statement execution");
  }

  @Test
  @DisplayName("Execute should handle null parameters gracefully")
  void executeShouldHandleNullParametersGracefully() throws SQLException {
    String operationName = "testOperation";
    SqlOperationSpec spec = createQuerySpec("SELECT 1", Collections.emptyList());

    setupSuccessfulQueryExecution(operationName, spec, "SELECT 1");

    SqlOperationResult result = operationManager.execute(operationName, null);

    assertNotNull(result);
    assertTrue(result.isSuccess());
  }

  @Test
  @DisplayName("Execute should handle empty result sets")
  void executeShouldHandleEmptyResultSets() throws SQLException {
    String operationName = "testOperation";
    String sql = "SELECT id FROM users WHERE 1=0";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(true);
    when(mockPreparedStatement.getResultSet()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.getColumnCount()).thenReturn(1);
    when(mockMetaData.getColumnLabel(1)).thenReturn("id");
    when(mockResultSet.next()).thenReturn(false); // No data
    when(mockPreparedStatement.getMoreResults()).thenReturn(false);
    when(mockPreparedStatement.getUpdateCount()).thenReturn(-1);

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());
    List<List<Object>> resultSet = result.getResultSets().get(0);
    assertEquals(1, resultSet.size()); // Only header row
    assertEquals("id", resultSet.get(0).get(0));
  }

  @Test
  @DisplayName("Execute should handle result sets with zero columns")
  void executeShouldHandleResultSetsWithZeroColumns() throws SQLException {
    String operationName = "testOperation";
    String sql = "SELECT 1 WHERE FALSE";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(true);
    when(mockPreparedStatement.getResultSet()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.getColumnCount()).thenReturn(0);
    when(mockPreparedStatement.getMoreResults()).thenReturn(false);
    when(mockPreparedStatement.getUpdateCount()).thenReturn(-1);

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getResultSets().size());
    assertTrue(result.getResultSets().get(0).isEmpty());
  }

  @Test
  @DisplayName("Execute procedure should handle transaction rollback on error")
  void executeProcedureShouldHandleTransactionRollbackOnError() throws SQLException {
    String operationName = "failingProcedure";
    String sql = "{call failing_procedure()}";
    SqlOperationSpec spec = createProcedureSpec(sql, List.of(createRefCursorParameter("cursor")));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenThrow(new SQLException("Procedure failed"));

    assertThrows(
        SQLException.class,
        () -> operationManager.execute(operationName, new Object[0]),
        "Should propagate SQLException from procedure execution");

    verify(mockConnection).setAutoCommit(false);
    verify(mockConnection, never()).commit(); // Should not commit on error
    verify(mockConnection).setAutoCommit(true); // Should restore auto-commit
  }

  @Test
  @DisplayName("Execute should handle output parameters without names")
  void executeShouldHandleOutputParametersWithoutNames() throws SQLException {
    String operationName = "procedure";
    String sql = "{call test_procedure(?)}";
    SqlParameter param =
        SqlParameter.builder()
            .type(SqlParameterType.STRING)
            .mode(SqlParameterMode.OUT)
            .build(); // No name specified

    SqlOperationSpec spec = createProcedureSpec(sql, List.of(param));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);
    when(mockCallableStatement.getObject(1)).thenReturn("output_value");

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(1, result.getOutputParameters().size());
    assertTrue(result.getOutputParameters().containsKey("out_1"));
    assertEquals("output_value", result.getOutputParameters().get("out_1"));
  }

  @Test
  @DisplayName("Execute should handle mixed parameter modes in procedures")
  void executeShouldHandleMixedParameterModesInProcedures() throws SQLException {
    String operationName = "mixedProcedure";
    String sql = "{call mixed_procedure(?, ?, ?, ?)}";
    SqlOperationSpec spec =
        createProcedureSpec(
            sql,
            List.of(
                createInParameter("input1", SqlParameterType.INTEGER),
                createOutParameter("output1", SqlParameterType.STRING),
                createInOutParameter("inout1", SqlParameterType.STRING),
                createInParameter("input2", SqlParameterType.INTEGER)));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);
    when(mockCallableStatement.getObject(2)).thenReturn("out_value");
    when(mockCallableStatement.getObject(3)).thenReturn("processed_inout");

    SqlOperationResult result =
        operationManager.execute(operationName, new Object[] {123, "inout_value", 456});

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(2, result.getOutputParameters().size());
    assertEquals("out_value", result.getOutputParameters().get("output1"));
    assertEquals("processed_inout", result.getOutputParameters().get("inout1"));

    // Verify parameter setting
    verify(mockCallableStatement).setInt(1, 123);
    verify(mockCallableStatement).setString(3, "inout_value");
    verify(mockCallableStatement).setInt(4, 456);

    // Verify output parameter registration
    verify(mockCallableStatement).registerOutParameter(2, Types.VARCHAR);
    verify(mockCallableStatement).registerOutParameter(3, Types.VARCHAR);
  }

  // Resource Management Tests
  @Test
  @DisplayName("Execute query should close resources on successful execution")
  void executeQueryShouldCloseResourcesOnSuccessfulExecution() throws SQLException {
    String operationName = "testQuery";
    String sql = "SELECT * FROM users";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    setupSuccessfulQueryExecution(operationName, spec, sql);

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());

    // Verify resources are closed
    verify(mockConnection).close();
    verify(mockPreparedStatement).close();
    verify(mockResultSet).close();
  }

  @Test
  @DisplayName("Execute query should close resources on SQLException during statement preparation")
  void executeQueryShouldCloseResourcesOnSQLExceptionDuringStatementPreparation()
      throws SQLException {
    String operationName = "testQuery";
    String sql = "INVALID SQL";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenThrow(new SQLException("SQL syntax error"));

    assertThrows(SQLException.class, () -> operationManager.execute(operationName, new Object[0]));

    // Connection should still be closed even on exception
    verify(mockConnection).close();
    // Statement was never created, so it shouldn't be closed
    verify(mockPreparedStatement, never()).close();
  }

  @Test
  @DisplayName("Execute query should close resources on SQLException during statement execution")
  void executeQueryShouldCloseResourcesOnSQLExceptionDuringStatementExecution()
      throws SQLException {
    String operationName = "testQuery";
    String sql = "SELECT * FROM non_existent_table";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenThrow(new SQLException("Table does not exist"));

    assertThrows(SQLException.class, () -> operationManager.execute(operationName, new Object[0]));

    // Both connection and statement should be closed
    verify(mockConnection).close();
    verify(mockPreparedStatement).close();
  }

  @Test
  @DisplayName("Execute query should close resources on SQLException during result set processing")
  void executeQueryShouldCloseResourcesOnSQLExceptionDuringResultSetProcessing()
      throws SQLException {
    String operationName = "testQuery";
    String sql = "SELECT * FROM users";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(true);
    when(mockPreparedStatement.getResultSet()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenThrow(new SQLException("ResultSet access error"));

    assertThrows(SQLException.class, () -> operationManager.execute(operationName, new Object[0]));

    // All resources should be closed
    verify(mockConnection).close();
    verify(mockPreparedStatement).close();
    verify(mockResultSet).close();
  }

  @Test
  @DisplayName("Execute procedure should close resources on successful execution")
  void executeProcedureShouldCloseResourcesOnSuccessfulExecution() throws SQLException {
    String operationName = "testProcedure";
    String sql = "{call test_procedure(?)}";
    SqlOperationSpec spec =
        createProcedureSpec(sql, List.of(createOutParameter("output", SqlParameterType.STRING)));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);
    when(mockCallableStatement.getObject(1)).thenReturn("output_value");

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());

    // Verify resources are closed
    verify(mockConnection).close();
    verify(mockCallableStatement).close();
  }

  @Test
  @DisplayName("Execute procedure should close resources on SQLException during execution")
  void executeProcedureShouldCloseResourcesOnSQLExceptionDuringExecution() throws SQLException {
    String operationName = "failingProcedure";
    String sql = "{call failing_procedure()}";
    SqlOperationSpec spec = createProcedureSpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenThrow(new SQLException("Procedure failed"));

    assertThrows(SQLException.class, () -> operationManager.execute(operationName, new Object[0]));

    // Both connection and statement should be closed even on exception
    verify(mockConnection).close();
    verify(mockCallableStatement).close();
    // Auto-commit should NOT be restored for procedures without REFCURSOR parameters
    verify(mockConnection, never()).setAutoCommit(true);
  }

  @Test
  @DisplayName("Execute procedure should close REFCURSOR result sets on successful execution")
  void executeProcedureShouldCloseRefCursorResultSetsOnSuccessfulExecution() throws SQLException {
    String operationName = "procedureWithCursor";
    String sql = "{call get_users_cursor(?)}";
    SqlOperationSpec spec =
        createProcedureSpec(sql, List.of(createRefCursorParameter("users_cursor")));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);

    // Mock REFCURSOR result set
    ResultSet mockCursorResultSet = mock(ResultSet.class);
    ResultSetMetaData mockCursorMetaData = mock(ResultSetMetaData.class);
    when(mockCallableStatement.getObject(1)).thenReturn(mockCursorResultSet);
    when(mockCursorResultSet.getMetaData()).thenReturn(mockCursorMetaData);
    when(mockCursorMetaData.getColumnCount()).thenReturn(1);
    when(mockCursorMetaData.getColumnLabel(1)).thenReturn("id");
    when(mockCursorResultSet.next()).thenReturn(false);

    SqlOperationResult result = operationManager.execute(operationName, new Object[0]);

    assertNotNull(result);
    assertTrue(result.isSuccess());

    // Verify all resources are closed including REFCURSOR result set
    verify(mockConnection).close();
    verify(mockCallableStatement).close();
    verify(mockCursorResultSet).close();
  }

  @Test
  @DisplayName("Execute procedure should close REFCURSOR result sets on SQLException")
  void executeProcedureShouldCloseRefCursorResultSetsOnSQLException() throws SQLException {
    String operationName = "procedureWithFailingCursor";
    String sql = "{call get_users_cursor(?)}";
    SqlOperationSpec spec =
        createProcedureSpec(sql, List.of(createRefCursorParameter("users_cursor")));

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getAutoCommit()).thenReturn(true);
    when(mockConnection.prepareCall(sql)).thenReturn(mockCallableStatement);
    when(mockCallableStatement.execute()).thenReturn(false);
    when(mockCallableStatement.getUpdateCount()).thenReturn(-1);
    when(mockCallableStatement.getMoreResults()).thenReturn(false);

    // Mock REFCURSOR result set that throws exception
    ResultSet mockCursorResultSet = mock(ResultSet.class);
    when(mockCallableStatement.getObject(1)).thenReturn(mockCursorResultSet);
    when(mockCursorResultSet.getMetaData()).thenThrow(new SQLException("REFCURSOR access error"));

    assertThrows(SQLException.class, () -> operationManager.execute(operationName, new Object[0]));

    // Verify all resources are closed even on exception
    verify(mockConnection).close();
    verify(mockCallableStatement).close();
    verify(mockCursorResultSet).close();
  }

  @Test
  @DisplayName("Execute should propagate connection close failure as SQLException")
  void executeShouldPropagateConnectionCloseFailureAsSQLException() throws SQLException {
    String operationName = "testQuery";
    String sql = "SELECT 1";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(true);
    when(mockPreparedStatement.getResultSet()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.getColumnCount()).thenReturn(1);
    when(mockMetaData.getColumnLabel(1)).thenReturn("col");
    when(mockResultSet.next()).thenReturn(false);
    when(mockPreparedStatement.getMoreResults()).thenReturn(false);
    when(mockPreparedStatement.getUpdateCount()).thenReturn(-1);

    // Make connection.close() throw an exception
    doThrow(new SQLException("Connection close failed")).when(mockConnection).close();

    // The operation should fail due to connection close failure
    SQLException exception =
        assertThrows(
            SQLException.class, () -> operationManager.execute(operationName, new Object[0]));

    assertEquals("Connection close failed", exception.getMessage());

    // Verify close was attempted
    verify(mockConnection).close();
    verify(mockPreparedStatement).close();
    verify(mockResultSet).close();
  }

  @Test
  @DisplayName("Execute should propagate statement close failure as SQLException")
  void executeShouldPropagateStatementCloseFailureAsSQLException() throws SQLException {
    String operationName = "testQuery";
    String sql = "SELECT 1";
    SqlOperationSpec spec = createQuerySpec(sql, Collections.emptyList());

    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(true);
    when(mockPreparedStatement.getResultSet()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.getColumnCount()).thenReturn(1);
    when(mockMetaData.getColumnLabel(1)).thenReturn("col");
    when(mockResultSet.next()).thenReturn(false);
    when(mockPreparedStatement.getMoreResults()).thenReturn(false);
    when(mockPreparedStatement.getUpdateCount()).thenReturn(-1);

    // Make statement.close() throw an exception
    doThrow(new SQLException("Statement close failed")).when(mockPreparedStatement).close();

    // The operation should fail due to statement close failure
    SQLException exception =
        assertThrows(
            SQLException.class, () -> operationManager.execute(operationName, new Object[0]));

    assertEquals("Statement close failed", exception.getMessage());

    // Verify close was attempted
    verify(mockPreparedStatement).close();
    verify(mockResultSet).close();
  }

  // Helper methods for creating test objects

  private SqlOperationSpec createQuerySpec(String sql, List<SqlParameter> parameters) {
    return SqlOperationSpec.builder()
        .sql(sql)
        .operationType(SqlOperationType.QUERY)
        .parameters(parameters)
        .build();
  }

  private SqlOperationSpec createProcedureSpec(String sql, List<SqlParameter> parameters) {
    return SqlOperationSpec.builder()
        .sql(sql)
        .operationType(SqlOperationType.PROCEDURE)
        .parameters(parameters)
        .build();
  }

  private SqlParameter createInParameter(String name, SqlParameterType type) {
    return SqlParameter.builder().name(name).type(type).mode(SqlParameterMode.IN).build();
  }

  private SqlParameter createOutParameter(String name, SqlParameterType type) {
    return SqlParameter.builder().name(name).type(type).mode(SqlParameterMode.OUT).build();
  }

  private SqlParameter createInOutParameter(String name, SqlParameterType type) {
    return SqlParameter.builder().name(name).type(type).mode(SqlParameterMode.INOUT).build();
  }

  private SqlParameter createRefCursorParameter(String name) {
    return SqlParameter.builder()
        .name(name)
        .type(SqlParameterType.REFCURSOR)
        .mode(SqlParameterMode.REFCURSOR)
        .build();
  }

  private void setupSuccessfulQueryExecution(
      String operationName, SqlOperationSpec spec, String sql) throws SQLException {
    when(mockRegistry.getOperationSpec(operationName)).thenReturn(spec);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.execute()).thenReturn(true);
    when(mockPreparedStatement.getResultSet()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.getColumnCount()).thenReturn(2);
    when(mockMetaData.getColumnLabel(1)).thenReturn("id");
    when(mockMetaData.getColumnLabel(2)).thenReturn("name");
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getObject(1)).thenReturn(123);
    when(mockResultSet.getObject(2)).thenReturn("John Doe");
    when(mockPreparedStatement.getMoreResults()).thenReturn(false);
    when(mockPreparedStatement.getUpdateCount()).thenReturn(-1);
  }
}
