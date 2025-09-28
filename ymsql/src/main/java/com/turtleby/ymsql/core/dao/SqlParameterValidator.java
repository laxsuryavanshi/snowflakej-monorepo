package com.turtleby.ymsql.core.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turtleby.ymsql.core.model.SqlOperationSpec;
import com.turtleby.ymsql.core.model.SqlParameter;

/**
 * Validates parameters for SQL operations according to their specifications.
 *
 * <p>This class is responsible for ensuring that the provided parameters match the expected
 * parameter definitions in terms of count for input parameters. Parameter values (including nulls)
 * are handled by the SqlParameterType when setting on SQL statements.
 */
class SqlParameterValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlParameterValidator.class);

  /**
   * Validates the parameters against the operation specification.
   *
   * @param operationName the name of the operation
   * @param spec the operation specification
   * @param parameters the parameters to validate
   * @throws SqlOperationValidationException if validation fails
   */
  public void validateParameters(
      final String operationName, final SqlOperationSpec spec, final Object[] parameters)
      throws IllegalArgumentException {
    final List<SqlParameter> expectedParameters = spec.getParameters();

    if (expectedParameters == null || expectedParameters.isEmpty()) {
      if (parameters != null && parameters.length > 0) {
        warnAdditionalParameters(operationName, 0, parameters.length);
      }
      return;
    }

    // Count only parameters that require input (IN and INOUT)
    final long inputParameterCount =
        expectedParameters.stream().filter(SqlParameter::requiresInput).count();

    final int expectedCount = (int) inputParameterCount;
    final int actualCount = parameters != null ? parameters.length : 0;

    if (actualCount < expectedCount) {
      throw new IllegalArgumentException(
          String.format(
              "Operation '%s' expects %d input parameters but %d were provided",
              operationName, expectedCount, actualCount));
    }
    if (actualCount > expectedCount) {
      warnAdditionalParameters(operationName, expectedCount, actualCount);
    }
  }

  private void warnAdditionalParameters(
      final String operationName, final int expectedCount, final int actualCount) {
    LOGGER.warn(
        "Operation '{}' expects {} input parameters but {} were provided. Additional parameters"
            + " will be ignored.",
        operationName,
        expectedCount,
        actualCount);
  }
}
