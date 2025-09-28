package com.turtleby.ymsql.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a SQL operation execution.
 *
 * <p>This class provides a unified response structure for both query and procedure results.
 *
 * <p>For queries, it contains the result set data. For procedures, it can contain both result sets
 * and output parameter values.
 */
public class SqlOperationResult {

  /**
   * List of result sets from the operation. Each result set is represented as a list of row maps.
   */
  private final List<List<List<Object>>> resultSets;

  /**
   * Output parameters from stored procedures. Map contains parameter index/name as key and value as
   * the parameter value.
   */
  private final Map<String, Object> outputParameters;

  /** Number of rows affected by the operation (for DML operations). */
  private final int affectedRows;

  /** Indicates whether the operation was successful. */
  private final boolean success;

  /** Error message if the operation failed. */
  private final String errorMessage;

  /** The SQL operation that was executed. */
  private final String executedSql;

  /** Execution time in milliseconds. */
  private final long executionTimeMs;

  public SqlOperationResult(
      final List<List<List<Object>>> resultSets,
      final Map<String, Object> outputParameters,
      final int affectedRows,
      final boolean success,
      final String errorMessage,
      final String executedSql,
      final long executionTimeMs) {
    this.resultSets = resultSets != null ? List.copyOf(resultSets) : null;
    this.outputParameters = outputParameters != null ? Map.copyOf(outputParameters) : null;
    this.affectedRows = affectedRows;
    this.success = success;
    this.errorMessage = errorMessage;
    this.executedSql = executedSql;
    this.executionTimeMs = executionTimeMs;
  }

  /**
   * Checks if the operation has any result sets.
   *
   * @return true if there are result sets, false otherwise
   */
  public boolean hasResultSets() {
    return !resultSets.isEmpty();
  }

  /**
   * Gets the total number of result sets.
   *
   * @return the number of result sets
   */
  public int getResultSetCount() {
    return resultSets.size();
  }

  /**
   * Checks if the operation has output parameters.
   *
   * @return true if there are output parameters, false otherwise
   */
  public boolean hasOutputParameters() {
    return !outputParameters.isEmpty();
  }

  /**
   * Gets the total number of output parameters.
   *
   * @return the number of output parameters
   */
  public int getOutputParameterCount() {
    return outputParameters.size();
  }

  public List<List<List<Object>>> getResultSets() {
    return resultSets;
  }

  public Map<String, Object> getOutputParameters() {
    return outputParameters;
  }

  public int getAffectedRows() {
    return affectedRows;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getExecutedSql() {
    return executedSql;
  }

  public long getExecutionTimeMs() {
    return executionTimeMs;
  }

  public static SqlOperationResultBuilder builder() {
    return new SqlOperationResultBuilder();
  }

  public static class SqlOperationResultBuilder {
    private List<List<List<Object>>> resultSets = new ArrayList<>();
    private Map<String, Object> outputParameters = new HashMap<>();
    private int affectedRows;
    private boolean success;
    private String errorMessage;
    private String executedSql;
    private long executionTimeMs;

    private SqlOperationResultBuilder() {}

    public SqlOperationResultBuilder resultSets(final List<List<List<Object>>> resultSets) {
      this.resultSets = resultSets;
      return this;
    }

    public SqlOperationResultBuilder outputParameters(final Map<String, Object> outputParameters) {
      this.outputParameters = outputParameters;
      return this;
    }

    public SqlOperationResultBuilder affectedRows(final int affectedRows) {
      this.affectedRows = affectedRows;
      return this;
    }

    public SqlOperationResultBuilder success(final boolean success) {
      this.success = success;
      return this;
    }

    public SqlOperationResultBuilder errorMessage(final String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public SqlOperationResultBuilder executedSql(final String executedSql) {
      this.executedSql = executedSql;
      return this;
    }

    public SqlOperationResultBuilder executionTimeMs(final long executionTimeMs) {
      this.executionTimeMs = executionTimeMs;
      return this;
    }

    public SqlOperationResultBuilder success(final String executedSql, final long executionTimeMs) {
      this.success = true;
      this.executedSql = executedSql;
      this.executionTimeMs = executionTimeMs;
      return this;
    }

    public SqlOperationResultBuilder failure(
        final String errorMessage, final String executedSql, final long executionTimeMs) {
      this.success = false;
      this.executedSql = executedSql;
      this.errorMessage = errorMessage;
      this.executionTimeMs = executionTimeMs;
      return this;
    }

    public SqlOperationResultBuilder addResultSet(final List<List<Object>> resultSet) {
      this.resultSets.add(resultSet);
      return this;
    }

    public SqlOperationResultBuilder addOutputParameter(final String key, final Object value) {
      this.outputParameters.put(key, value);
      return this;
    }

    public SqlOperationResult build() {
      return new SqlOperationResult(
          resultSets,
          outputParameters,
          affectedRows,
          success,
          errorMessage,
          executedSql,
          executionTimeMs);
    }
  }
}
