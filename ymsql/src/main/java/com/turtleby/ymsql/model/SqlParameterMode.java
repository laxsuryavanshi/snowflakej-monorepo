package com.turtleby.ymsql.model;

import java.sql.ParameterMetaData;

import org.springframework.util.Assert;

/**
 * Enumeration representing SQL parameter modes for stored procedures and functions. This enum
 * defines how parameters are used in callable statements.
 *
 * <p>Parameter modes include:
 *
 * <ul>
 *   <li>IN - Input parameter only
 *   <li>OUT - Output parameter only
 *   <li>INOUT - Both input and output parameter
 *   <li>REFCURSOR - PostgreSQL cursor reference (output only)
 * </ul>
 */
public enum SqlParameterMode {
  /** Input parameter - value is passed to the procedure/function. */
  IN(ParameterMetaData.parameterModeIn),

  /** Output parameter - value is returned from the procedure/function. */
  OUT(ParameterMetaData.parameterModeOut),

  /** Input/Output parameter - value is passed in and returned from the procedure/function. */
  INOUT(ParameterMetaData.parameterModeInOut),

  /**
   * PostgreSQL REFCURSOR parameter - returns a cursor reference to a result set. This is treated as
   * an output parameter but requires special handling to retrieve the actual ResultSet.
   */
  REFCURSOR(ParameterMetaData.parameterModeOut);

  private final int jdbcMode;

  SqlParameterMode(final int jdbcMode) {
    this.jdbcMode = jdbcMode;
  }

  /**
   * Gets the JDBC parameter mode constant.
   *
   * @return the JDBC parameter mode constant
   */
  public int getJdbcMode() {
    return jdbcMode;
  }

  /**
   * Checks if this parameter mode represents a cursor reference.
   *
   * @return true if this is a REFCURSOR parameter
   */
  public boolean isRefCursor() {
    return this == REFCURSOR;
  }

  /**
   * Checks if this parameter mode is an output parameter (OUT, INOUT, or REFCURSOR).
   *
   * @return true if this parameter produces output
   */
  public boolean isOutput() {
    return this == OUT || this == INOUT || this == REFCURSOR;
  }

  /**
   * Checks if this parameter mode is an input parameter (IN or INOUT).
   *
   * @return true if this parameter accepts input
   */
  public boolean isInput() {
    return this == IN || this == INOUT;
  }

  /**
   * Converts a JDBC parameter mode constant to SqlParameterMode.
   *
   * @param jdbcMode the JDBC parameter mode constant
   * @return the corresponding SqlParameterMode
   * @throws IllegalArgumentException if the mode is not supported
   */
  public static SqlParameterMode fromJdbcMode(final int jdbcMode) {
    return switch (jdbcMode) {
      case ParameterMetaData.parameterModeIn -> IN;
      case ParameterMetaData.parameterModeOut -> OUT;
      case ParameterMetaData.parameterModeInOut -> INOUT;
      default -> throw new IllegalArgumentException("Unsupported parameter mode: " + jdbcMode);
    };
  }

  /**
   * Converts a string representation to SqlParameterMode.
   *
   * @param mode the string representation (case-insensitive)
   * @return the corresponding SqlParameterMode, or IN if not found or null
   */
  public static SqlParameterMode fromString(final String mode) {
    Assert.hasText(mode, "Parameter mode string must not be null or empty");

    return switch (mode.trim().toUpperCase()) {
      case "IN" -> IN;
      case "OUT" -> OUT;
      case "INOUT", "IN_OUT" -> INOUT;
      case "REFCURSOR", "REF_CURSOR" -> REFCURSOR;
      default -> throw new IllegalArgumentException("Unknown parameter mode: " + mode);
    };
  }
}
