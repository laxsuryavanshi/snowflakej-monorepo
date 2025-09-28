package com.turtleby.ymsql.core.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turtleby.ymsql.core.model.SqlOperationResult;
import com.turtleby.ymsql.core.model.SqlOperationResult.SqlOperationResultBuilder;
import com.turtleby.ymsql.core.model.SqlOperationSpec;
import com.turtleby.ymsql.core.model.SqlParameter;
import com.turtleby.ymsql.core.model.SqlParameterMode;

public class SqlOperationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlOperationManager.class);

  private final SqlOperationRegistry sqlOperationRegistry;
  private final DataSource dataSource;

  private final SqlParameterValidator parameterValidator = new SqlParameterValidator();

  public SqlOperationManager(
      final SqlOperationRegistry sqlOperationRegistry, final DataSource dataSource) {
    this.sqlOperationRegistry =
        Objects.requireNonNull(sqlOperationRegistry, "SqlOperationRegistry must not be null");
    this.dataSource = Objects.requireNonNull(dataSource, "DataSource must not be null");
  }

  /**
   * Executes a SQL operation with the given parameters.
   *
   * @param operationName the name of the operation to execute
   * @param parameters the parameters for the operation
   * @return the result of the operation
   */
  public SqlOperationResult execute(final String operationName, final Object[] parameters)
      throws SQLException {
    if (StringUtils.isBlank(operationName)) {
      throw new IllegalArgumentException("Operation name must not be null or empty");
    }

    // Retrieve operation specification
    final SqlOperationSpec operationSpec = sqlOperationRegistry.getOperationSpec(operationName);

    // Validate parameters
    parameterValidator.validateParameters(operationName, operationSpec, parameters);

    LOGGER.debug(
        "Executing SQL operation '{}' with {} parameters",
        operationName,
        parameters != null ? parameters.length : 0);

    // Execute the operation
    final long startTime = System.currentTimeMillis();
    final SqlOperationResultBuilder resultBuilder = executeOperation(operationSpec, parameters);

    final long executionTime = System.currentTimeMillis() - startTime;
    resultBuilder.executionTimeMs(executionTime);

    LOGGER.debug("SQL operation '{}' completed in {}ms", operationName, executionTime);

    return resultBuilder.build();
  }

  private SqlOperationResultBuilder executeOperation(
      final SqlOperationSpec spec, final Object[] parameters) throws SQLException {
    try (final Connection connection = dataSource.getConnection()) {
      return switch (spec.getOperationType()) {
        case QUERY -> executeQuery(connection, spec, parameters);
        case PROCEDURE -> executeProcedure(connection, spec, parameters);
      };
    }
  }

  /** Handles execution of query operations. */
  private SqlOperationResultBuilder executeQuery(
      final Connection connection, final SqlOperationSpec spec, final Object[] parameters)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(spec.getSql())) {
      setParameters(statement, spec, parameters);

      final boolean hasResultSet = statement.execute();
      final SqlOperationResultBuilder resultBuilder = processResultSets(statement, hasResultSet);

      return resultBuilder.success(spec.getSql(), 0);
    }
  }

  /** Handles execution of procedure operations. */
  private SqlOperationResultBuilder executeProcedure(
      final Connection connection, final SqlOperationSpec spec, final Object[] parameters)
      throws SQLException {
    final SqlProcedureTransactionManager txManager =
        new SqlProcedureTransactionManager(connection, spec);

    try {
      txManager.beginIfNeeded();

      try (CallableStatement statement = connection.prepareCall(spec.getSql())) {
        setParameters(statement, spec, parameters);

        final boolean hasResultSet = statement.execute();
        final SqlOperationResultBuilder resultBuilder = processResultSets(statement, hasResultSet);
        processOutputParameters(statement, spec, resultBuilder);

        txManager.commitIfNeeded();

        return resultBuilder.success(spec.getSql(), 0);
      }
    } finally {
      txManager.restoreAutoCommit();
    }
  }

  private SqlOperationResultBuilder processResultSets(
      final PreparedStatement statement, final boolean executeResult) throws SQLException {
    final SqlOperationResultBuilder resultBuilder = SqlOperationResult.builder();
    int affectedRows = 0;

    boolean hasResultSet = executeResult;
    do {
      if (hasResultSet) {
        try (ResultSet resultSet = statement.getResultSet()) {
          final List<List<Object>> resultData = extractResultSetData(resultSet);
          resultBuilder.addResultSet(resultData);
        }
      } else {
        final int updateCount = statement.getUpdateCount();
        if (updateCount >= 0) {
          affectedRows += updateCount;
        }
      }
      hasResultSet = statement.getMoreResults();
    } while (hasResultSet || statement.getUpdateCount() != -1);

    return resultBuilder.affectedRows(affectedRows);
  }

  /**
   * Processes output parameters from a callable statement.
   *
   * @param statement the callable statement
   * @param spec the operation specification
   * @param resultBuilder the operation result builder to populate
   * @throws SQLException if parameter processing fails
   */
  private void processOutputParameters(
      final CallableStatement statement,
      final SqlOperationSpec spec,
      final SqlOperationResultBuilder resultBuilder)
      throws SQLException {
    final List<SqlParameter> parameterSpecs = spec.getParameters();
    if (parameterSpecs == null || parameterSpecs.isEmpty()) {
      return;
    }

    for (int i = 0; i < parameterSpecs.size(); i++) {
      final SqlParameter paramSpec = parameterSpecs.get(i);
      final int jdbcParamIndex = i + 1;

      if (paramSpec.producesOutput()) {
        processOutputParameter(statement, paramSpec, jdbcParamIndex, resultBuilder);
      }
    }
  }

  private void processOutputParameter(
      final CallableStatement statement,
      final SqlParameter paramSpec,
      final int jdbcParamIndex,
      final SqlOperationResultBuilder resultBuilder)
      throws SQLException {
    if (paramSpec.getMode() == SqlParameterMode.REFCURSOR) {
      processRefCursorParameter(statement, paramSpec, jdbcParamIndex, resultBuilder);
    } else {
      processRegularOutputParameter(statement, paramSpec, jdbcParamIndex, resultBuilder);
    }
  }

  /** Processes a REFCURSOR output parameter. */
  private void processRefCursorParameter(
      final CallableStatement statement,
      final SqlParameter paramSpec,
      final int jdbcParamIndex,
      final SqlOperationResultBuilder resultBuilder)
      throws SQLException {
    try (ResultSet cursorResultSet = (ResultSet) statement.getObject(jdbcParamIndex)) {
      final List<List<Object>> cursorData = extractResultSetData(cursorResultSet);
      final String paramName = getParameterName(paramSpec, jdbcParamIndex);
      resultBuilder.addOutputParameter(paramName, cursorData);

      LOGGER.debug("Processed REFCURSOR parameter {} with {} rows", paramName, cursorData.size());
    }
  }

  /** Processes a regular (non-REFCURSOR) output parameter. */
  private void processRegularOutputParameter(
      final CallableStatement statement,
      final SqlParameter paramSpec,
      final int jdbcParamIndex,
      final SqlOperationResultBuilder resultBuilder)
      throws SQLException {
    final Object outputValue = statement.getObject(jdbcParamIndex);
    final String paramName = getParameterName(paramSpec, jdbcParamIndex);
    resultBuilder.addOutputParameter(paramName, outputValue);

    LOGGER.debug(
        "Processed {} parameter {} with value: {}", paramSpec.getMode(), paramName, outputValue);
  }

  private String getParameterName(final SqlParameter paramSpec, final int jdbcParamIndex) {
    return StringUtils.isNotBlank(paramSpec.getName())
        ? paramSpec.getName()
        : "out_" + jdbcParamIndex;
  }

  /**
   * Sets parameters on a PreparedStatement for query operations.
   *
   * @param statement the prepared statement
   * @param spec the operation specification
   * @param parameters the parameter values
   * @throws SQLException if parameter setting fails
   */
  private void setParameters(
      final PreparedStatement statement, final SqlOperationSpec spec, final Object[] parameters)
      throws SQLException {
    if (parameters == null || parameters.length == 0) {
      return;
    }

    final List<SqlParameter> parameterSpecs = spec.getParameters();

    for (int i = 0; i < parameterSpecs.size(); i++) {
      final SqlParameter paramSpec = parameterSpecs.get(i);

      paramSpec.getType().setParameter(statement, i + 1, parameters[i]);
    }
  }

  /**
   * Sets parameters on a CallableStatement for procedure operations.
   *
   * @param statement the callable statement
   * @param spec the operation specification
   * @param parameters the parameter values
   * @throws SQLException if parameter setting fails
   */
  private void setParameters(
      final CallableStatement statement, final SqlOperationSpec spec, final Object[] parameters)
      throws SQLException {
    final List<SqlParameter> parameterSpecs = spec.getParameters();
    if (parameterSpecs == null || parameterSpecs.isEmpty()) {
      return;
    }

    int inputParamIndex = 0;

    for (int i = 0; i < parameterSpecs.size(); i++) {
      final int jdbcParamIndex = i + 1;
      final SqlParameter paramSpec = parameterSpecs.get(i);

      if (paramSpec.requiresInput()) {
        if (parameters != null && inputParamIndex < parameters.length) {
          final Object paramValue = parameters[inputParamIndex];
          paramSpec.getType().setParameter(statement, jdbcParamIndex, paramValue);
          inputParamIndex++;
        } else {
          throw new IllegalArgumentException("Missing input parameter at index " + inputParamIndex);
        }
      }

      if (paramSpec.producesOutput()) {
        paramSpec.getType().registerOutParameter(statement, jdbcParamIndex);
      }
    }
  }

  /**
   * Extracts result set data from JDBC ResultSet.
   *
   * @param rs the JDBC ResultSet
   * @return a list of rows, where first row is column names and subsequent rows are data
   * @throws SQLException if database access error occurs
   */
  private List<List<Object>> extractResultSetData(final ResultSet rs) throws SQLException {
    final var metaData = rs.getMetaData();
    final int columnCount = metaData.getColumnCount();

    if (columnCount <= 0) {
      return new LinkedList<>();
    }

    final List<List<Object>> rows = new LinkedList<>();
    final List<Object> columnNames = new ArrayList<>(columnCount);

    for (int i = 0; i < columnCount; ++i) {
      columnNames.add(metaData.getColumnLabel(i + 1));
    }

    rows.add(columnNames);

    while (rs.next()) {
      final List<Object> row = new ArrayList<>(columnCount);
      for (int i = 1; i <= columnCount; i++) {
        final Object value = rs.getObject(i);
        row.add(value);
      }
      rows.add(row);
    }

    return rows;
  }
}
