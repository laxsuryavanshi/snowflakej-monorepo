package com.turtleby.ymsql.model;

import java.util.List;
import java.util.Objects;

import org.springframework.util.Assert;

/**
 * Specification for a SQL operation including the SQL statement, operation type, and parameter
 * definitions.
 *
 * <p>This class represents a complete specification for executing a database operation, typically
 * loaded from YAML configuration files. It encapsulates the SQL statement text, the type of
 * operation (QUERY or PROCEDURE), and detailed parameter specifications.
 */
public class SqlOperationSpec {
  /** The SQL statement or stored procedure call text. */
  private String sql;

  /** The type of SQL operation (QUERY or PROCEDURE). */
  private SqlOperationType operationType;

  /** List of parameter specifications for the SQL operation. */
  private List<SqlParameter> parameters;

  public SqlOperationSpec() {}

  public SqlOperationSpec(
      final String sql, final SqlOperationType operationType, final List<SqlParameter> parameters) {
    this.sql = sql;
    this.operationType = operationType;
    this.parameters = parameters;
  }

  public SqlOperationSpec(final SqlOperationSpecBuilder builder) {
    this(builder.sql, builder.operationType, builder.parameters);
  }

  public String getSql() {
    return sql;
  }

  public void setSql(final String sql) {
    this.sql = sql;
  }

  public SqlOperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(final SqlOperationType operationType) {
    this.operationType = operationType;
  }

  public List<SqlParameter> getParameters() {
    return parameters;
  }

  public void setParameters(final List<SqlParameter> parameters) {
    this.parameters = parameters;
  }

  /**
   * Convenience method to check if this operation is a query.
   *
   * @return true if this is a QUERY operation, false otherwise
   */
  public boolean isQuery() {
    return operationType == SqlOperationType.QUERY;
  }

  /**
   * Convenience method to check if this operation is a stored procedure.
   *
   * @return true if this is a PROCEDURE operation, false otherwise
   */
  public boolean isProcedure() {
    return operationType == SqlOperationType.PROCEDURE;
  }

  @Override
  public String toString() {
    return "SqlOperationSpec{"
        + "sql='"
        + sql
        + "', operationType="
        + operationType
        + ", parameters="
        + parameters
        + '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final SqlOperationSpec that = (SqlOperationSpec) o;

    if (!Objects.equals(sql, that.sql)) {
      return false;
    }
    if (operationType != that.operationType) {
      return false;
    }
    return Objects.equals(parameters, that.parameters);
  }

  @Override
  public int hashCode() {
    int result = sql != null ? sql.hashCode() : 0;
    result = 31 * result + (operationType != null ? operationType.hashCode() : 0);
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    return result;
  }

  /**
   * Enumeration of supported SQL operation types.
   *
   * <p>This enum defines the types of database operations that can be specified in a
   * SqlOperationSpec. Each type corresponds to a different execution pattern and expected result
   * handling.
   */
  public static enum SqlOperationType {
    QUERY("query"),
    PROCEDURE("procedure");

    SqlOperationType(final String value) {}
  }

  /**
   * Create a builder for constructing SqlOperationSpec instances.
   *
   * @return a new builder instance
   */
  public static SqlOperationSpecBuilder builder() {
    return new SqlOperationSpecBuilder();
  }

  public static class SqlOperationSpecBuilder {
    private String sql;
    private SqlOperationType operationType;
    private List<SqlParameter> parameters;

    private SqlOperationSpecBuilder() {}

    public SqlOperationSpecBuilder sql(final String sql) {
      this.sql = sql;
      return this;
    }

    public SqlOperationSpecBuilder operationType(final SqlOperationType operationType) {
      this.operationType = operationType;
      return this;
    }

    public SqlOperationSpecBuilder parameters(final List<SqlParameter> parameters) {
      this.parameters = parameters;
      return this;
    }

    public SqlOperationSpec build() {
      Assert.hasText(sql, "SQL must not be empty");
      Assert.notNull(operationType, "Operation type must not be null");
      return new SqlOperationSpec(this);
    }
  }
}
