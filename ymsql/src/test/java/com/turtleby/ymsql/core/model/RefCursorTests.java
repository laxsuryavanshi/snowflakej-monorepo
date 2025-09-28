package com.turtleby.ymsql.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("REFCURSOR Parameter Tests")
class RefCursorTests {
  @Mock private CallableStatement callableStatement;

  @Mock private PreparedStatement preparedStatement;

  @Mock private ResultSet resultSet;

  @Nested
  @DisplayName("SqlParameterMode REFCURSOR Tests")
  class SqlParameterModeRefCursorTests {
    @Test
    @DisplayName("REFCURSOR mode should have correct JDBC mode")
    void testRefCursorJdbcMode() {
      assertEquals(ParameterMetaData.parameterModeOut, SqlParameterMode.REFCURSOR.getJdbcMode());
    }

    @Test
    @DisplayName("REFCURSOR should be identified as cursor")
    void testRefCursorIsRefCursor() {
      assertTrue(SqlParameterMode.REFCURSOR.isRefCursor());
      assertFalse(SqlParameterMode.IN.isRefCursor());
      assertFalse(SqlParameterMode.OUT.isRefCursor());
      assertFalse(SqlParameterMode.INOUT.isRefCursor());
    }

    @Test
    @DisplayName("REFCURSOR should be output parameter")
    void testRefCursorIsOutput() {
      assertTrue(SqlParameterMode.REFCURSOR.isOutput());
      assertTrue(SqlParameterMode.OUT.isOutput());
      assertTrue(SqlParameterMode.INOUT.isOutput());
      assertFalse(SqlParameterMode.IN.isOutput());
    }

    @Test
    @DisplayName("REFCURSOR should not be input parameter")
    void testRefCursorIsNotInput() {
      assertFalse(SqlParameterMode.REFCURSOR.isInput());
      assertTrue(SqlParameterMode.IN.isInput());
      assertTrue(SqlParameterMode.INOUT.isInput());
      assertFalse(SqlParameterMode.OUT.isInput());
    }

    @Test
    @DisplayName("Should parse REFCURSOR from string")
    void testFromStringRefCursor() {
      assertEquals(SqlParameterMode.REFCURSOR, SqlParameterMode.fromString("REFCURSOR"));
      assertEquals(SqlParameterMode.REFCURSOR, SqlParameterMode.fromString("refcursor"));
      assertEquals(SqlParameterMode.REFCURSOR, SqlParameterMode.fromString("REF_CURSOR"));
      assertEquals(SqlParameterMode.REFCURSOR, SqlParameterMode.fromString("ref_cursor"));
      assertEquals(SqlParameterMode.REFCURSOR, SqlParameterMode.fromString(" REFCURSOR "));
    }

    @Test
    @DisplayName("Should handle unknown parameter modes")
    void testFromStringUnknown() {
      Exception exception =
          assertThrows(
              IllegalArgumentException.class, () -> SqlParameterMode.fromString("INVALID"));
      assertTrue(exception.getMessage().contains("Unknown parameter mode: INVALID"));
    }
  }

  @Nested
  @DisplayName("SqlParameterType REFCURSOR Tests")
  class SqlParameterTypeRefCursorTests {
    @Test
    @DisplayName("REFCURSOR should have correct SQL type")
    void testRefCursorSqlType() {
      assertEquals(Types.REF_CURSOR, SqlParameterType.REFCURSOR.getSqlType());
    }

    @Test
    @DisplayName("REFCURSOR should be created from string aliases")
    void testFromStringRefCursor() {
      assertEquals(SqlParameterType.REFCURSOR, SqlParameterType.fromString("refcursor"));
      assertEquals(SqlParameterType.REFCURSOR, SqlParameterType.fromString("REFCURSOR"));
      assertEquals(SqlParameterType.REFCURSOR, SqlParameterType.fromString("ref_cursor"));
      assertEquals(SqlParameterType.REFCURSOR, SqlParameterType.fromString("REF_CURSOR"));
    }

    @Test
    @DisplayName("REFCURSOR should throw exception with PreparedStatement")
    void testRefCursorWithPreparedStatement() throws SQLException {
      SQLException exception =
          assertThrows(
              SQLException.class,
              () -> SqlParameterType.REFCURSOR.setParameter(preparedStatement, 1, null));

      assertTrue(
          exception
              .getMessage()
              .contains("REFCURSOR is typically used as OUT parameter in CallableStatement"));
    }

    @Test
    @DisplayName("REFCURSOR should register as output parameter with REF_CURSOR type")
    void testRefCursorRegisterOutParameter() throws SQLException {
      SqlParameterType.REFCURSOR.registerOutParameter(callableStatement, 1);

      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
    }

    @Test
    @DisplayName("REFCURSOR should fallback to OTHER type if REF_CURSOR not supported")
    void testRefCursorRegisterOutParameterFallback() throws SQLException {
      // Mock REF_CURSOR not supported
      doThrow(new SQLException("REF_CURSOR not supported"))
          .when(callableStatement)
          .registerOutParameter(1, Types.REF_CURSOR);

      SqlParameterType.REFCURSOR.registerOutParameter(callableStatement, 1);

      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
      verify(callableStatement).registerOutParameter(1, Types.OTHER);
    }

    @Test
    @DisplayName("REFCURSOR with CallableStatement should only register for output modes")
    void testRefCursorWithCallableStatementOutputModes() throws SQLException {
      // Test with REFCURSOR mode
      SqlParameterType.REFCURSOR.setCallableParameter(
          callableStatement, 1, null, SqlParameterMode.REFCURSOR);
      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);

      // Reset mock
      reset(callableStatement);

      // Test with OUT mode
      SqlParameterType.REFCURSOR.setCallableParameter(
          callableStatement, 1, null, SqlParameterMode.OUT);
      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
    }

    @Test
    @DisplayName("REFCURSOR should throw exception for input modes")
    void testRefCursorWithInputModes() {
      SQLException exception1 =
          assertThrows(
              SQLException.class,
              () ->
                  SqlParameterType.REFCURSOR.setCallableParameter(
                      callableStatement, 1, "value", SqlParameterMode.IN));
      assertTrue(exception1.getMessage().contains("REFCURSOR cannot be used as input parameter"));

      SQLException exception2 =
          assertThrows(
              SQLException.class,
              () ->
                  SqlParameterType.REFCURSOR.setCallableParameter(
                      callableStatement, 1, "value", SqlParameterMode.INOUT));
      assertTrue(exception2.getMessage().contains("REFCURSOR cannot be used as input parameter"));
    }
  }

  @Nested
  @DisplayName("REFCURSOR Integration Tests")
  class RefCursorIntegrationTests {
    @Test
    @DisplayName("Should handle REFCURSOR in switch statement for parameter modes")
    void testRefCursorInParameterModeSwitch() throws SQLException {
      // This tests the switch statement in setCallableParameter method
      SqlParameterType.REFCURSOR.setCallableParameter(
          callableStatement, 1, null, SqlParameterMode.REFCURSOR);

      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
      verifyNoMoreInteractions(callableStatement);
    }

    @Test
    @DisplayName("Should include REFCURSOR in getSqlType switch expression")
    void testRefCursorInGetSqlTypeSwitch() {
      // This tests that REFCURSOR is included in the switch expression
      int sqlType = SqlParameterType.REFCURSOR.getSqlType();
      assertEquals(Types.REF_CURSOR, sqlType);
    }

    @Test
    @DisplayName("Should validate REFCURSOR parameter mode combinations")
    void testRefCursorModeValidation() {
      SqlParameterMode mode = SqlParameterMode.REFCURSOR;

      // REFCURSOR should be output-only
      assertTrue(mode.isOutput());
      assertFalse(mode.isInput());
      assertTrue(mode.isRefCursor());

      // JDBC mode should be OUT
      assertEquals(ParameterMetaData.parameterModeOut, mode.getJdbcMode());
    }

    @Test
    @DisplayName("Should handle REFCURSOR with database metadata")
    void testRefCursorWithMetadata() throws SQLException {
      when(callableStatement.getObject(1)).thenReturn(resultSet);
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString(1)).thenReturn("test_value");

      // Simulate registering and getting REFCURSOR
      SqlParameterType.REFCURSOR.registerOutParameter(callableStatement, 1);
      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);

      // Simulate execution and result retrieval
      Object result = callableStatement.getObject(1);
      assertNotNull(result);
      assertInstanceOf(ResultSet.class, result);

      // Use try-with-resources to properly handle ResultSet (though it's a mock)
      try (ResultSet rs = (ResultSet) result) {
        assertTrue(rs.next());
        assertEquals("test_value", rs.getString(1));
      }
    }

    @Test
    @DisplayName("Should handle REFCURSOR error scenarios")
    void testRefCursorErrorHandling() throws SQLException {
      // Test SQL exception during registration
      doThrow(new SQLException("Database connection lost"))
          .when(callableStatement)
          .registerOutParameter(anyInt(), anyInt());

      assertThrows(
          SQLException.class,
          () -> SqlParameterType.REFCURSOR.registerOutParameter(callableStatement, 1));
    }
  }

  @Nested
  @DisplayName("REFCURSOR Cross-Database Support Tests")
  class RefCursorCrossDatabaseTests {
    @Test
    @DisplayName("Should support PostgreSQL REFCURSOR")
    void testPostgreSQLRefCursor() throws SQLException {
      // PostgreSQL supports Types.REF_CURSOR natively
      SqlParameterType.REFCURSOR.registerOutParameter(callableStatement, 1);
      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
    }

    @Test
    @DisplayName("Should fallback for databases without REF_CURSOR support")
    void testDatabaseWithoutRefCursorSupport() throws SQLException {
      // Simulate database that doesn't support REF_CURSOR
      doThrow(new SQLException("Unsupported SQL type: REF_CURSOR"))
          .when(callableStatement)
          .registerOutParameter(1, Types.REF_CURSOR);

      SqlParameterType.REFCURSOR.registerOutParameter(callableStatement, 1);

      // Should attempt REF_CURSOR first, then fallback to OTHER
      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
      verify(callableStatement).registerOutParameter(1, Types.OTHER);
    }

    @Test
    @DisplayName("Should handle Oracle-style SYS_REFCURSOR")
    void testOracleStyleRefCursor() throws SQLException {
      // Oracle typically uses Types.REF_CURSOR for SYS_REFCURSOR
      SqlParameterType.REFCURSOR.registerOutParameter(callableStatement, 1);
      verify(callableStatement).registerOutParameter(1, Types.REF_CURSOR);
    }
  }

  @Nested
  @DisplayName("REFCURSOR String Parsing Tests")
  class RefCursorStringParsingTests {
    @Test
    @DisplayName("Should parse various REFCURSOR string formats")
    void testRefCursorStringFormats() {
      String[] validFormats = {
        "refcursor", "REFCURSOR", "RefCursor", "ref_cursor",
        "REF_CURSOR", "Ref_Cursor", " refcursor ", " REF_CURSOR "
      };

      for (String format : validFormats) {
        assertEquals(
            SqlParameterType.REFCURSOR,
            SqlParameterType.fromString(format),
            "Failed to parse format: " + format);
        assertEquals(
            SqlParameterMode.REFCURSOR,
            SqlParameterMode.fromString(format),
            "Failed to parse mode format: " + format);
      }
    }

    @Test
    @DisplayName("Should reject invalid REFCURSOR string formats")
    void testInvalidRefCursorFormats() {
      String[] invalidFormats = {"cursor", "reference", "refcurs", "cursors", "invalid_type"};

      for (String format : invalidFormats) {
        // SqlParameterType.fromString doesn't throw exceptions, it returns OBJECT for
        // unknown types
        SqlParameterType result = SqlParameterType.fromString(format);
        assertNotEquals(
            SqlParameterType.REFCURSOR,
            result,
            "Should not parse invalid format as REFCURSOR: " + format);
        assertEquals(
            SqlParameterType.OBJECT, result, "Should return OBJECT for unknown format: " + format);

        // SqlParameterMode.fromString does throw exceptions for unknown types
        assertThrows(
            IllegalArgumentException.class,
            () -> SqlParameterMode.fromString(format),
            "Should reject invalid mode format: " + format);
      }
    }
  }
}
