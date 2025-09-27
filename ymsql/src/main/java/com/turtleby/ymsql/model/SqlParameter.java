package com.turtleby.ymsql.model;

import java.util.Objects;

import org.springframework.util.Assert;

/**
 * Represents a parameter specification for SQL operations including stored procedures.
 *
 * <p>This class defines the metadata for a single parameter in a SQL operation, including its data
 * type, parameter mode (IN/OUT/INOUT), and optional name for documentation purposes. Parameters are
 * used to specify the expected inputs and outputs for parameterized queries and stored procedure
 * calls.
 *
 * <p>Usage Examples:
 *
 * <pre>{@code
 * // Simple input parameter
 * SqlParameter inputParam = SqlParameter.builder()
 *     .name("userId")
 *     .type(SqlParameterType.INTEGER)
 *     .mode(SqlParameterMode.IN)
 *     .build();
 *
 * // Output parameter for stored procedure
 * SqlParameter outputParam = SqlParameter.builder()
 *     .name("resultCursor")
 *     .type(SqlParameterType.REFCURSOR)
 *     .mode(SqlParameterMode.OUT)
 *     .build();
 *
 * // INOUT parameter
 * SqlParameter inoutParam = SqlParameter.builder()
 *     .type(SqlParameterType.VARCHAR)
 *     .mode(SqlParameterMode.INOUT)
 *     .build();
 * }</pre>
 *
 * <p>This class is mutable for framework convenience but should be treated as immutable after
 * initial configuration.
 */
public class SqlParameter {
  private static final String SEPARATOR = ":";

  /** The parameter name (optional, for documentation purposes). */
  private String name;

  /** The parameter data type. */
  private SqlParameterType type;

  /** The parameter mode (IN/OUT/INOUT/REFCURSOR). */
  private SqlParameterMode mode = SqlParameterMode.IN;

  public SqlParameter() {}

  public SqlParameter(final String name, final SqlParameterType type, final SqlParameterMode mode) {
    this.name = name;
    this.type = type;
    this.mode = mode;
  }

  public SqlParameter(final SqlParameterBuilder builder) {
    this(builder.name, builder.type, builder.mode);
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public SqlParameterType getType() {
    return type;
  }

  public void setType(final SqlParameterType type) {
    this.type = type;
  }

  public SqlParameterMode getMode() {
    return mode;
  }

  public void setMode(final SqlParameterMode mode) {
    this.mode = mode;
  }

  /** Check if this parameter requires input value. */
  public boolean requiresInput() {
    return mode == SqlParameterMode.IN || mode == SqlParameterMode.INOUT;
  }

  /** Check if this parameter produces output value. */
  public boolean producesOutput() {
    return mode == SqlParameterMode.OUT
        || mode == SqlParameterMode.INOUT
        || mode == SqlParameterMode.REFCURSOR;
  }

  @Override
  public String toString() {
    return "SqlParameter{" + "name='" + name + '\'' + ", type=" + type + ", mode=" + mode + '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final SqlParameter that = (SqlParameter) o;

    if (!Objects.equals(name, that.name)) {
      return false;
    }
    if (type != that.type) {
      return false;
    }
    return mode == that.mode;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (mode != null ? mode.hashCode() : 0);
    return result;
  }

  /**
   * Factory method to create a SqlParameter from a string representation.
   *
   * <p>The string format is "type", "type:mode" or "name:type:mode".
   *
   * @param param the string representation of the parameter
   * @return the SqlParameter instance
   * @throws IllegalArgumentException if the format is invalid
   */
  public static SqlParameter fromString(final String param) {
    Assert.notNull(param, "Parameter string cannot be null");

    final String[] parts = param.split(SEPARATOR);
    final SqlParameterBuilder builder = SqlParameter.builder();

    switch (parts.length) {
      case 1:
        builder.type(SqlParameterType.valueOf(parts[0].trim().toUpperCase()));
        break;
      case 2:
        builder.type(SqlParameterType.valueOf(parts[0].trim().toUpperCase()));
        builder.mode(SqlParameterMode.valueOf(parts[1].trim().toUpperCase()));
        break;
      case 3:
        builder.name(parts[0]);
        builder.type(SqlParameterType.valueOf(parts[1].trim().toUpperCase()));
        builder.mode(SqlParameterMode.valueOf(parts[2].trim().toUpperCase()));
        break;
      default:
        throw new IllegalArgumentException("Invalid parameter string format: " + param);
    }

    return builder.build();
  }

  /** Factory method to create an IN parameter. */
  public static SqlParameter in(final SqlParameterType type) {
    return SqlParameter.builder().type(type).mode(SqlParameterMode.IN).build();
  }

  /** Factory method to create a named IN parameter. */
  public static SqlParameter in(final String name, final SqlParameterType type) {
    return SqlParameter.builder().name(name).type(type).mode(SqlParameterMode.IN).build();
  }

  /** Factory method to create an OUT parameter. */
  public static SqlParameter out(final SqlParameterType type) {
    return SqlParameter.builder().type(type).mode(SqlParameterMode.OUT).build();
  }

  /** Factory method to create a named OUT parameter. */
  public static SqlParameter out(final String name, final SqlParameterType type) {
    return SqlParameter.builder().name(name).type(type).mode(SqlParameterMode.OUT).build();
  }

  /** Factory method to create an INOUT parameter. */
  public static SqlParameter inout(final SqlParameterType type) {
    return SqlParameter.builder().type(type).mode(SqlParameterMode.INOUT).build();
  }

  /** Factory method to create a named INOUT parameter. */
  public static SqlParameter inout(final String name, final SqlParameterType type) {
    return SqlParameter.builder().name(name).type(type).mode(SqlParameterMode.INOUT).build();
  }

  /** Factory method to create a REFCURSOR parameter. */
  public static SqlParameter refcursor() {
    return SqlParameter.builder()
        .type(SqlParameterType.REFCURSOR)
        .mode(SqlParameterMode.REFCURSOR)
        .build();
  }

  /** Factory method to create a named REFCURSOR parameter. */
  public static SqlParameter refcursor(final String name) {
    return SqlParameter.builder()
        .name(name)
        .type(SqlParameterType.REFCURSOR)
        .mode(SqlParameterMode.REFCURSOR)
        .build();
  }

  /** Create a builder for SqlParameter. */
  public static SqlParameterBuilder builder() {
    return new SqlParameterBuilder();
  }

  public static class SqlParameterBuilder {
    private String name;
    private SqlParameterType type;
    private SqlParameterMode mode = SqlParameterMode.IN;

    private SqlParameterBuilder() {}

    public SqlParameterBuilder name(final String name) {
      this.name = name;
      return this;
    }

    public SqlParameterBuilder type(final SqlParameterType type) {
      this.type = type;
      return this;
    }

    public SqlParameterBuilder mode(final SqlParameterMode mode) {
      this.mode = mode;
      return this;
    }

    public SqlParameter build() {
      Assert.notNull(type, "Parameter type cannot be null");
      return new SqlParameter(this);
    }
  }
}
