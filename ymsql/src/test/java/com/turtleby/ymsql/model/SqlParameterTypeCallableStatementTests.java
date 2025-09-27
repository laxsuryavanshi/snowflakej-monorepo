package com.turtleby.ymsql.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqlParameterType CallableStatement Tests")
public class SqlParameterTypeCallableStatementTests {
  @Mock private CallableStatement callableStatement;

  @Nested
  @DisplayName("CallableStatement Parameter Setting Tests")
  class CallableStatementParameterTests {
    @Test
    @DisplayName("Should set IN parameter correctly")
    void shouldSetInParameterCorrectly() throws SQLException {
      // Given
      String value = "test value";
      SqlParameterMode mode = SqlParameterMode.IN;

      // When
      SqlParameterType.STRING.setCallableParameter(callableStatement, 1, value, mode);

      // Then
      verify(callableStatement).setString(1, value);
    }

    @Test
    @DisplayName("Should register OUT parameter correctly")
    void shouldRegisterOutParameterCorrectly() throws SQLException {
      // Given
      SqlParameterMode mode = SqlParameterMode.OUT;

      // When
      SqlParameterType.STRING.setCallableParameter(callableStatement, 1, null, mode);

      // Then
      verify(callableStatement).registerOutParameter(1, Types.VARCHAR);
    }

    @Test
    @DisplayName("Should handle INOUT parameter correctly")
    void shouldHandleInOutParameterCorrectly() throws SQLException {
      // Given
      String value = "test value";
      SqlParameterMode mode = SqlParameterMode.INOUT;

      // When
      SqlParameterType.STRING.setCallableParameter(callableStatement, 1, value, mode);

      // Then
      verify(callableStatement).setString(1, value);
      verify(callableStatement).registerOutParameter(1, Types.VARCHAR);
    }

    @Test
    @DisplayName("Should register out parameter with correct SQL type")
    void shouldRegisterOutParameterWithCorrectSqlType() throws SQLException {
      // When
      SqlParameterType.INTEGER.registerOutParameter(callableStatement, 1);

      // Then
      verify(callableStatement).registerOutParameter(1, Types.INTEGER);
    }
  }

  @Nested
  @DisplayName("SQL Type Mapping Tests")
  class SqlTypeMappingTests {
    @ParameterizedTest
    @MethodSource("provideSqlTypeMappings")
    @DisplayName("Should return correct SQL type for each parameter type")
    void shouldReturnCorrectSqlTypeForEachParameterType(
        SqlParameterType type, int expectedSqlType) {
      // When
      int actualSqlType = type.getSqlType();

      // Then
      assertThat(actualSqlType).isEqualTo(expectedSqlType);
    }

    private static Stream<Arguments> provideSqlTypeMappings() {
      return Stream.of(
          Arguments.of(SqlParameterType.STRING, Types.VARCHAR),
          Arguments.of(SqlParameterType.INTEGER, Types.INTEGER),
          Arguments.of(SqlParameterType.LONG, Types.BIGINT),
          Arguments.of(SqlParameterType.SHORT, Types.SMALLINT),
          Arguments.of(SqlParameterType.BYTE, Types.TINYINT),
          Arguments.of(SqlParameterType.FLOAT, Types.FLOAT),
          Arguments.of(SqlParameterType.DOUBLE, Types.DOUBLE),
          Arguments.of(SqlParameterType.DECIMAL, Types.DECIMAL),
          Arguments.of(SqlParameterType.BOOLEAN, Types.BOOLEAN),
          Arguments.of(SqlParameterType.DATE, Types.DATE),
          Arguments.of(SqlParameterType.TIME, Types.TIME),
          Arguments.of(SqlParameterType.TIMESTAMP, Types.TIMESTAMP),
          Arguments.of(SqlParameterType.BLOB, Types.BLOB),
          Arguments.of(SqlParameterType.CLOB, Types.CLOB),
          Arguments.of(SqlParameterType.NCLOB, Types.CLOB),
          Arguments.of(SqlParameterType.BYTES, Types.VARBINARY),
          Arguments.of(SqlParameterType.ARRAY, Types.ARRAY),
          Arguments.of(SqlParameterType.URL, Types.DATALINK),
          Arguments.of(SqlParameterType.ROWID, Types.ROWID),
          Arguments.of(SqlParameterType.SQLXML, Types.SQLXML),
          Arguments.of(SqlParameterType.REF, Types.REF),
          Arguments.of(SqlParameterType.UUID, Types.OTHER),
          Arguments.of(SqlParameterType.JSON, Types.OTHER),
          Arguments.of(SqlParameterType.NCHAR, Types.VARCHAR),
          Arguments.of(SqlParameterType.OBJECT, Types.JAVA_OBJECT));
    }
  }

  @Nested
  @DisplayName("Parameter Mode Integration Tests")
  class ParameterModeIntegrationTests {
    @ParameterizedTest
    @EnumSource(SqlParameterMode.class)
    @DisplayName("Should handle all parameter modes correctly")
    void shouldHandleAllParameterModesCorrectly(SqlParameterMode mode) throws SQLException {
      // Given
      String value = "test";

      // When & Then - should not throw exception
      SqlParameterType.STRING.setCallableParameter(callableStatement, 1, value, mode);

      // Verify appropriate method calls based on mode
      switch (mode) {
        case IN -> verify(callableStatement).setString(1, value);
        case OUT -> verify(callableStatement).registerOutParameter(1, Types.VARCHAR);
        case INOUT -> {
          verify(callableStatement).setString(1, value);
          verify(callableStatement).registerOutParameter(1, Types.VARCHAR);
        }
        case REFCURSOR -> verify(callableStatement).registerOutParameter(1, Types.VARCHAR);
      }
    }
  }

  @Nested
  @DisplayName("Complex Type CallableStatement Tests")
  class ComplexTypeCallableStatementTests {
    @Test
    @DisplayName("Should handle BLOB type with CallableStatement")
    void shouldHandleBlobTypeWithCallableStatement() throws SQLException {
      // Given
      byte[] data = "test data".getBytes();

      // When
      SqlParameterType.BLOB.setCallableParameter(
          callableStatement, 1, data, SqlParameterMode.INOUT);

      // Then
      verify(callableStatement).setBlob(eq(1), any(java.io.InputStream.class));
      verify(callableStatement).registerOutParameter(1, Types.BLOB);
    }

    @Test
    @DisplayName("Should handle INTEGER type with CallableStatement")
    void shouldHandleIntegerTypeWithCallableStatement() throws SQLException {
      // Given
      int value = 42;

      // When
      SqlParameterType.INTEGER.setCallableParameter(
          callableStatement, 1, value, SqlParameterMode.IN);

      // Then
      verify(callableStatement).setInt(1, value);
    }

    @Test
    @DisplayName("Should handle null values correctly for OUT parameters")
    void shouldHandleNullValuesCorrectlyForOutParameters() throws SQLException {
      // When
      SqlParameterType.STRING.setCallableParameter(
          callableStatement, 1, null, SqlParameterMode.OUT);

      // Then
      verify(callableStatement).registerOutParameter(1, Types.VARCHAR);
      // Should not call setString when mode is OUT
    }
  }
}
