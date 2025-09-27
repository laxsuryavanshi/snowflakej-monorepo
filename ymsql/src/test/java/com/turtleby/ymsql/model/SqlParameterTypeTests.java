package com.turtleby.ymsql.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Base64;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqlParameterType Tests")
public class SqlParameterTypeTests {
  @Mock private PreparedStatement preparedStatement;

  @Mock private Connection connection;

  @BeforeEach
  void setUp() throws SQLException {
    // Connection setup only when needed
  }

  @Nested
  @DisplayName("String Type Tests")
  class StringTypeTests {
    @Test
    @DisplayName("Should set string parameter correctly")
    void shouldSetStringParameter() throws SQLException {
      // Given
      String value = "test string";

      // When
      SqlParameterType.STRING.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setString(1, value);
    }

    @Test
    @DisplayName("Should set null for null string value")
    void shouldSetNullForNullStringValue() throws SQLException {
      // When
      SqlParameterType.STRING.setParameter(preparedStatement, 1, null);

      // Then
      verify(preparedStatement).setNull(1, Types.VARCHAR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"varchar", "text", "char", "string"})
    @DisplayName("Should recognize string aliases")
    void shouldRecognizeStringAliases(String alias) {
      // When
      SqlParameterType type = SqlParameterType.fromString(alias);

      // Then
      assertThat(type).isEqualTo(SqlParameterType.STRING);
    }
  }

  @Nested
  @DisplayName("Integer Type Tests")
  class IntegerTypeTests {
    @Test
    @DisplayName("Should set integer parameter correctly")
    void shouldSetIntegerParameter() throws SQLException {
      // Given
      int value = 42;

      // When
      SqlParameterType.INTEGER.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setInt(1, value);
    }

    @Test
    @DisplayName("Should parse string to integer")
    void shouldParseStringToInteger() throws SQLException {
      // Given
      String value = "123";

      // When
      SqlParameterType.INTEGER.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setInt(1, 123);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid integer")
    void shouldThrowSQLExceptionForInvalidInteger() {
      // Given
      String invalidValue = "not_a_number";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.INTEGER.setParameter(preparedStatement, 1, invalidValue))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid integer value");
    }

    @Test
    @DisplayName("Should set null for null integer value")
    void shouldSetNullForNullIntegerValue() throws SQLException {
      // When
      SqlParameterType.INTEGER.setParameter(preparedStatement, 1, null);

      // Then
      verify(preparedStatement).setNull(1, Types.INTEGER);
    }
  }

  @Nested
  @DisplayName("Long Type Tests")
  class LongTypeTests {
    @Test
    @DisplayName("Should set long parameter correctly")
    void shouldSetLongParameter() throws SQLException {
      // Given
      long value = 123456789L;

      // When
      SqlParameterType.LONG.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setLong(1, value);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid long")
    void shouldThrowSQLExceptionForInvalidLong() {
      // Given
      String invalidValue = "not_a_long";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.LONG.setParameter(preparedStatement, 1, invalidValue))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid long value");
    }
  }

  @Nested
  @DisplayName("Boolean Type Tests")
  class BooleanTypeTests {
    @ParameterizedTest
    @CsvSource({"true, true", "false, false", "TRUE, true", "FALSE, false", "1, false", "0, false"})
    @DisplayName("Should parse boolean values correctly")
    void shouldParseBooleanValuesCorrectly(String input, boolean expected) throws SQLException {
      // When
      SqlParameterType.BOOLEAN.setParameter(preparedStatement, 1, input);

      // Then
      verify(preparedStatement).setBoolean(1, expected);
    }
  }

  @Nested
  @DisplayName("Date Type Tests")
  class DateTypeTests {
    @Test
    @DisplayName("Should set Date parameter correctly")
    void shouldSetDateParameter() throws SQLException {
      // Given
      Date date = Date.valueOf("2023-12-25");

      // When
      SqlParameterType.DATE.setParameter(preparedStatement, 1, date);

      // Then
      verify(preparedStatement).setDate(1, date);
    }

    @Test
    @DisplayName("Should parse string to Date")
    void shouldParseStringToDate() throws SQLException {
      // Given
      String dateString = "2023-12-25";

      // When
      SqlParameterType.DATE.setParameter(preparedStatement, 1, dateString);

      // Then
      verify(preparedStatement).setDate(1, Date.valueOf(dateString));
    }

    @Test
    @DisplayName("Should throw SQLException for invalid date format")
    void shouldThrowSQLExceptionForInvalidDateFormat() {
      // Given
      String invalidDate = "invalid-date";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.DATE.setParameter(preparedStatement, 1, invalidDate))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid date format");
    }
  }

  @Nested
  @DisplayName("Time Type Tests")
  class TimeTypeTests {
    @Test
    @DisplayName("Should set Time parameter correctly")
    void shouldSetTimeParameter() throws SQLException {
      // Given
      Time time = Time.valueOf("14:30:00");

      // When
      SqlParameterType.TIME.setParameter(preparedStatement, 1, time);

      // Then
      verify(preparedStatement).setTime(1, time);
    }

    @Test
    @DisplayName("Should parse string to Time")
    void shouldParseStringToTime() throws SQLException {
      // Given
      String timeString = "14:30:00";

      // When
      SqlParameterType.TIME.setParameter(preparedStatement, 1, timeString);

      // Then
      verify(preparedStatement).setTime(1, Time.valueOf(timeString));
    }

    @Test
    @DisplayName("Should throw SQLException for invalid time format")
    void shouldThrowSQLExceptionForInvalidTimeFormat() {
      // Given
      String invalidTime = "invalid-time";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.TIME.setParameter(preparedStatement, 1, invalidTime))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid time format");
    }
  }

  @Nested
  @DisplayName("Timestamp Type Tests")
  class TimestampTypeTests {
    @Test
    @DisplayName("Should set Timestamp parameter correctly")
    void shouldSetTimestampParameter() throws SQLException {
      // Given
      Timestamp timestamp = Timestamp.valueOf("2023-12-25 14:30:00");

      // When
      SqlParameterType.TIMESTAMP.setParameter(preparedStatement, 1, timestamp);

      // Then
      verify(preparedStatement).setTimestamp(1, timestamp);
    }

    @Test
    @DisplayName("Should parse string to Timestamp")
    void shouldParseStringToTimestamp() throws SQLException {
      // Given
      String timestampString = "2023-12-25 14:30:00";

      // When
      SqlParameterType.TIMESTAMP.setParameter(preparedStatement, 1, timestampString);

      // Then
      verify(preparedStatement).setTimestamp(1, Timestamp.valueOf(timestampString));
    }

    @Test
    @DisplayName("Should throw SQLException for invalid timestamp format")
    void shouldThrowSQLExceptionForInvalidTimestampFormat() {
      // Given
      String invalidTimestamp = "invalid-timestamp";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.TIMESTAMP.setParameter(preparedStatement, 1, invalidTimestamp))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid timestamp format");
    }
  }

  @Nested
  @DisplayName("Decimal Type Tests")
  class DecimalTypeTests {
    @Test
    @DisplayName("Should set BigDecimal parameter correctly")
    void shouldSetBigDecimalParameter() throws SQLException {
      // Given
      BigDecimal decimal = new BigDecimal("123.45");

      // When
      SqlParameterType.DECIMAL.setParameter(preparedStatement, 1, decimal);

      // Then
      verify(preparedStatement).setBigDecimal(1, decimal);
    }

    @Test
    @DisplayName("Should parse string to BigDecimal")
    void shouldParseStringToBigDecimal() throws SQLException {
      // Given
      String decimalString = "123.45";

      // When
      SqlParameterType.DECIMAL.setParameter(preparedStatement, 1, decimalString);

      // Then
      verify(preparedStatement).setBigDecimal(1, new BigDecimal(decimalString));
    }

    @Test
    @DisplayName("Should throw SQLException for invalid decimal")
    void shouldThrowSQLExceptionForInvalidDecimal() {
      // Given
      String invalidDecimal = "not_a_decimal";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.DECIMAL.setParameter(preparedStatement, 1, invalidDecimal))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid decimal value");
    }
  }

  @Nested
  @DisplayName("Blob Type Tests")
  class BlobTypeTests {
    @Test
    @DisplayName("Should set Blob parameter correctly")
    void shouldSetBlobParameter() throws SQLException {
      // Given
      Blob blob = mock(Blob.class);

      // When
      SqlParameterType.BLOB.setParameter(preparedStatement, 1, blob);

      // Then
      verify(preparedStatement).setBlob(1, blob);
    }

    @Test
    @DisplayName("Should set byte array as Blob")
    void shouldSetByteArrayAsBlob() throws SQLException {
      // Given
      byte[] bytes = "test".getBytes();

      // When
      SqlParameterType.BLOB.setParameter(preparedStatement, 1, bytes);

      // Then
      verify(preparedStatement).setBlob(eq(1), any(ByteArrayInputStream.class));
    }

    @Test
    @DisplayName("Should decode base64 string to Blob")
    void shouldDecodeBase64StringToBlob() throws SQLException {
      // Given
      String base64 = Base64.getEncoder().encodeToString("test".getBytes());

      // When
      SqlParameterType.BLOB.setParameter(preparedStatement, 1, base64);

      // Then
      verify(preparedStatement).setBlob(eq(1), any(ByteArrayInputStream.class));
    }

    @Test
    @DisplayName("Should set null for null Blob value")
    void shouldSetNullForNullBlobValue() throws SQLException {
      // When
      SqlParameterType.BLOB.setParameter(preparedStatement, 1, null);

      // Then
      verify(preparedStatement).setNull(1, Types.BLOB);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid base64")
    void shouldThrowSQLExceptionForInvalidBase64() {
      // Given
      String invalidBase64 = "invalid_base64!@#";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.BLOB.setParameter(preparedStatement, 1, invalidBase64))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid base64 string");
    }
  }

  @Nested
  @DisplayName("Clob Type Tests")
  class ClobTypeTests {
    @Test
    @DisplayName("Should set Clob parameter correctly")
    void shouldSetClobParameter() throws SQLException {
      // Given
      Clob clob = mock(Clob.class);

      // When
      SqlParameterType.CLOB.setParameter(preparedStatement, 1, clob);

      // Then
      verify(preparedStatement).setClob(1, clob);
    }

    @Test
    @DisplayName("Should set string as Clob")
    void shouldSetStringAsClob() throws SQLException {
      // Given
      String text = "test text";

      // When
      SqlParameterType.CLOB.setParameter(preparedStatement, 1, text);

      // Then
      verify(preparedStatement).setClob(eq(1), any(StringReader.class));
    }

    @Test
    @DisplayName("Should set null for null Clob value")
    void shouldSetNullForNullClobValue() throws SQLException {
      // When
      SqlParameterType.CLOB.setParameter(preparedStatement, 1, null);

      // Then
      verify(preparedStatement).setNull(1, Types.CLOB);
    }
  }

  @Nested
  @DisplayName("Bytes Type Tests")
  class BytesTypeTests {
    @Test
    @DisplayName("Should set byte array parameter correctly")
    void shouldSetByteArrayParameter() throws SQLException {
      // Given
      byte[] bytes = "test".getBytes();

      // When
      SqlParameterType.BYTES.setParameter(preparedStatement, 1, bytes);

      // Then
      verify(preparedStatement).setBytes(1, bytes);
    }

    @Test
    @DisplayName("Should decode base64 string to bytes")
    void shouldDecodeBase64StringToBytes() throws SQLException {
      // Given
      String base64 = Base64.getEncoder().encodeToString("test".getBytes());

      // When
      SqlParameterType.BYTES.setParameter(preparedStatement, 1, base64);

      // Then
      verify(preparedStatement).setBytes(eq(1), any(byte[].class));
    }

    @Test
    @DisplayName("Should decode hex string to bytes")
    void shouldDecodeHexStringToBytes() throws SQLException {
      // Given
      String hex = "48656c6c6f"; // "Hello" in hex

      // When
      SqlParameterType.BYTES.setParameter(preparedStatement, 1, hex);

      // Then
      verify(preparedStatement).setBytes(eq(1), any(byte[].class));
    }

    @Test
    @DisplayName("Should handle empty hex string")
    void shouldHandleEmptyHexString() throws SQLException {
      // Given
      String emptyHex = "";

      // When
      SqlParameterType.BYTES.setParameter(preparedStatement, 1, emptyHex);

      // Then
      verify(preparedStatement).setBytes(1, new byte[0]);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid hex string length")
    void shouldThrowSQLExceptionForInvalidHexStringLength() {
      // Given - odd length hex string
      String invalidHex = "48656c6c6";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.BYTES.setParameter(preparedStatement, 1, invalidHex))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid byte data format");
    }
  }

  @Nested
  @DisplayName("URL Type Tests")
  class URLTypeTests {
    @Test
    @DisplayName("Should set URL parameter correctly")
    void shouldSetURLParameter() throws SQLException, MalformedURLException {
      // Given
      URL url = java.net.URI.create("https://example.com").toURL();

      // When
      SqlParameterType.URL.setParameter(preparedStatement, 1, url);

      // Then
      verify(preparedStatement).setURL(1, url);
    }

    @Test
    @DisplayName("Should parse string to URL")
    void shouldParseStringToURL() throws SQLException {
      // Given
      String urlString = "https://example.com";

      // When
      SqlParameterType.URL.setParameter(preparedStatement, 1, urlString);

      // Then
      verify(preparedStatement).setURL(eq(1), any(URL.class));
    }

    @Test
    @DisplayName("Should throw SQLException for invalid URL")
    void shouldThrowSQLExceptionForInvalidURL() {
      // Given
      String invalidUrl = "not a valid url";

      // When & Then
      assertThatThrownBy(() -> SqlParameterType.URL.setParameter(preparedStatement, 1, invalidUrl))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid URL format");
    }
  }

  @Nested
  @DisplayName("SQLXML Type Tests")
  class SQLXMLTypeTests {
    @Test
    @DisplayName("Should set SQLXML parameter correctly")
    void shouldSetSQLXMLParameter() throws SQLException {
      // Given
      SQLXML sqlxml = mock(SQLXML.class);

      // When
      SqlParameterType.SQLXML.setParameter(preparedStatement, 1, sqlxml);

      // Then
      verify(preparedStatement).setSQLXML(1, sqlxml);
    }

    @Test
    @DisplayName("Should create SQLXML from string")
    void shouldCreateSQLXMLFromString() throws SQLException {
      // Given
      String xmlString = "<root>test</root>";
      SQLXML sqlxml = mock(SQLXML.class);
      when(preparedStatement.getConnection()).thenReturn(connection);
      when(connection.createSQLXML()).thenReturn(sqlxml);
      doNothing().when(sqlxml).setString(xmlString);

      // When
      SqlParameterType.SQLXML.setParameter(preparedStatement, 1, xmlString);

      // Then
      verify(connection).createSQLXML();
      verify(sqlxml).setString(xmlString);
      verify(preparedStatement).setSQLXML(1, sqlxml);
    }

    @Test
    @DisplayName("Should handle SQLXML creation failure and cleanup")
    void shouldHandleSQLXMLCreationFailureAndCleanup() throws SQLException {
      // Given
      String xmlString = "<root>test</root>";
      SQLXML sqlxml = mock(SQLXML.class);
      when(preparedStatement.getConnection()).thenReturn(connection);
      when(connection.createSQLXML()).thenReturn(sqlxml);
      doThrow(new RuntimeException("XML parsing error")).when(sqlxml).setString(xmlString);

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.SQLXML.setParameter(preparedStatement, 1, xmlString))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Failed to set XML value");

      verify(sqlxml).free();
    }

    @Test
    @DisplayName("Should handle SQLXML cleanup failure")
    void shouldHandleSQLXMLCleanupFailure() throws SQLException {
      // Given
      String xmlString = "<root>test</root>";
      SQLXML sqlxml = mock(SQLXML.class);
      when(preparedStatement.getConnection()).thenReturn(connection);
      when(connection.createSQLXML()).thenReturn(sqlxml);
      doThrow(new RuntimeException("XML parsing error")).when(sqlxml).setString(xmlString);
      doThrow(new SQLException("Cleanup failed")).when(sqlxml).free();

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.SQLXML.setParameter(preparedStatement, 1, xmlString))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Failed to set XML value");

      verify(sqlxml).free();
    }
  }

  @Nested
  @DisplayName("Array Type Tests")
  class ArrayTypeTests {
    @Test
    @DisplayName("Should set Array parameter correctly")
    void shouldSetArrayParameter() throws SQLException {
      // Given
      Array array = mock(Array.class);

      // When
      SqlParameterType.ARRAY.setParameter(preparedStatement, 1, array);

      // Then
      verify(preparedStatement).setArray(1, array);
    }

    @Test
    @DisplayName("Should set object for non-Array types")
    void shouldSetObjectForNonArrayTypes() throws SQLException {
      // Given
      Object value = new String[] {"a", "b", "c"};

      // When
      SqlParameterType.ARRAY.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setObject(1, value);
    }
  }

  @Nested
  @DisplayName("fromString Method Tests")
  class FromStringMethodTests {
    @ParameterizedTest
    @MethodSource("provideTypeAliases")
    @DisplayName("Should map aliases to correct types")
    void shouldMapAliasesToCorrectTypes(String alias, SqlParameterType expectedType) {
      // When
      SqlParameterType actualType = SqlParameterType.fromString(alias);

      // Then
      assertThat(actualType).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Should return OBJECT for null, empty or whitespace strings")
    void shouldReturnObjectForNullEmptyOrWhitespaceStrings(String input) {
      // When
      SqlParameterType type = SqlParameterType.fromString(input);

      // Then
      assertThat(type).isEqualTo(SqlParameterType.OBJECT);
    }

    @Test
    @DisplayName("Should return OBJECT for unknown type")
    void shouldReturnObjectForUnknownType() {
      // When
      SqlParameterType type = SqlParameterType.fromString("unknown_type");

      // Then
      assertThat(type).isEqualTo(SqlParameterType.OBJECT);
    }

    @Test
    @DisplayName("Should be case insensitive")
    void shouldBeCaseInsensitive() {
      // When
      SqlParameterType lowerCase = SqlParameterType.fromString("varchar");
      SqlParameterType upperCase = SqlParameterType.fromString("VARCHAR");
      SqlParameterType mixedCase = SqlParameterType.fromString("VarChar");

      // Then
      assertThat(lowerCase).isEqualTo(SqlParameterType.STRING);
      assertThat(upperCase).isEqualTo(SqlParameterType.STRING);
      assertThat(mixedCase).isEqualTo(SqlParameterType.STRING);
    }

    private static Stream<Arguments> provideTypeAliases() {
      return Stream.of(
          Arguments.of("string", SqlParameterType.STRING),
          Arguments.of("varchar", SqlParameterType.STRING),
          Arguments.of("text", SqlParameterType.STRING),
          Arguments.of("char", SqlParameterType.STRING),
          Arguments.of("int", SqlParameterType.INTEGER),
          Arguments.of("integer", SqlParameterType.INTEGER),
          Arguments.of("number", SqlParameterType.INTEGER),
          Arguments.of("long", SqlParameterType.LONG),
          Arguments.of("bigint", SqlParameterType.LONG),
          Arguments.of("short", SqlParameterType.SHORT),
          Arguments.of("smallint", SqlParameterType.SHORT),
          Arguments.of("byte", SqlParameterType.BYTE),
          Arguments.of("tinyint", SqlParameterType.BYTE),
          Arguments.of("float", SqlParameterType.FLOAT),
          Arguments.of("real", SqlParameterType.FLOAT),
          Arguments.of("double", SqlParameterType.DOUBLE),
          Arguments.of("double precision", SqlParameterType.DOUBLE),
          Arguments.of("numeric", SqlParameterType.DOUBLE),
          Arguments.of("decimal", SqlParameterType.DECIMAL),
          Arguments.of("money", SqlParameterType.DECIMAL),
          Arguments.of("boolean", SqlParameterType.BOOLEAN),
          Arguments.of("bool", SqlParameterType.BOOLEAN),
          Arguments.of("bit", SqlParameterType.BOOLEAN),
          Arguments.of("date", SqlParameterType.DATE),
          Arguments.of("time", SqlParameterType.TIME),
          Arguments.of("timestamp", SqlParameterType.TIMESTAMP),
          Arguments.of("datetime", SqlParameterType.TIMESTAMP),
          Arguments.of("blob", SqlParameterType.BLOB),
          Arguments.of("longblob", SqlParameterType.BLOB),
          Arguments.of("clob", SqlParameterType.CLOB),
          Arguments.of("longtext", SqlParameterType.CLOB),
          Arguments.of("nclob", SqlParameterType.NCLOB),
          Arguments.of("ntext", SqlParameterType.NCLOB),
          Arguments.of("bytes", SqlParameterType.BYTES),
          Arguments.of("varbinary", SqlParameterType.BYTES),
          Arguments.of("binary", SqlParameterType.BYTES),
          Arguments.of("array", SqlParameterType.ARRAY),
          Arguments.of("url", SqlParameterType.URL),
          Arguments.of("uri", SqlParameterType.URL),
          Arguments.of("rowid", SqlParameterType.ROWID),
          Arguments.of("xml", SqlParameterType.SQLXML),
          Arguments.of("sqlxml", SqlParameterType.SQLXML),
          Arguments.of("ref", SqlParameterType.REF),
          Arguments.of("sqlref", SqlParameterType.REF),
          Arguments.of("uuid", SqlParameterType.UUID),
          Arguments.of("json", SqlParameterType.JSON),
          Arguments.of("jsonb", SqlParameterType.JSON),
          Arguments.of("nchar", SqlParameterType.NCHAR),
          Arguments.of("nvarchar", SqlParameterType.NCHAR),
          Arguments.of("object", SqlParameterType.OBJECT));
    }
  }

  @Nested
  @DisplayName("Numeric Type Edge Cases")
  class NumericTypeEdgeCasesTests {
    @Test
    @DisplayName("Should handle SHORT type correctly")
    void shouldHandleShortTypeCorrectly() throws SQLException {
      // Given
      short value = 32767;

      // When
      SqlParameterType.SHORT.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setShort(1, value);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid short")
    void shouldThrowSQLExceptionForInvalidShort() {
      // Given
      String invalidValue = "not_a_short";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.SHORT.setParameter(preparedStatement, 1, invalidValue))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid short value");
    }

    @Test
    @DisplayName("Should handle BYTE type correctly")
    void shouldHandleByteTypeCorrectly() throws SQLException {
      // Given
      byte value = 127;

      // When
      SqlParameterType.BYTE.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setByte(1, value);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid byte")
    void shouldThrowSQLExceptionForInvalidByte() {
      // Given
      String invalidValue = "not_a_byte";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.BYTE.setParameter(preparedStatement, 1, invalidValue))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid byte value");
    }

    @Test
    @DisplayName("Should handle FLOAT type correctly")
    void shouldHandleFloatTypeCorrectly() throws SQLException {
      // Given
      float value = 3.14f;

      // When
      SqlParameterType.FLOAT.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setFloat(1, value);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid float")
    void shouldThrowSQLExceptionForInvalidFloat() {
      // Given
      String invalidValue = "not_a_float";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.FLOAT.setParameter(preparedStatement, 1, invalidValue))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid float value");
    }

    @Test
    @DisplayName("Should handle DOUBLE type correctly")
    void shouldHandleDoubleTypeCorrectly() throws SQLException {
      // Given
      double value = 3.14159;

      // When
      SqlParameterType.DOUBLE.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setDouble(1, value);
    }

    @Test
    @DisplayName("Should throw SQLException for invalid double")
    void shouldThrowSQLExceptionForInvalidDouble() {
      // Given
      String invalidValue = "not_a_double";

      // When & Then
      assertThatThrownBy(
              () -> SqlParameterType.DOUBLE.setParameter(preparedStatement, 1, invalidValue))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("Invalid double value");
    }
  }

  @Nested
  @DisplayName("Special Type Tests")
  class SpecialTypeTests {
    @Test
    @DisplayName("Should handle UUID type")
    void shouldHandleUUIDType() throws SQLException {
      // Given
      String uuid = "123e4567-e89b-12d3-a456-426614174000";

      // When
      SqlParameterType.UUID.setParameter(preparedStatement, 1, uuid);

      // Then
      verify(preparedStatement).setObject(1, uuid);
    }

    @Test
    @DisplayName("Should handle JSON type")
    void shouldHandleJSONType() throws SQLException {
      // Given
      String json = "{\"key\": \"value\"}";

      // When
      SqlParameterType.JSON.setParameter(preparedStatement, 1, json);

      // Then
      verify(preparedStatement).setObject(1, json);
    }

    @Test
    @DisplayName("Should handle NCHAR type")
    void shouldHandleNCHARType() throws SQLException {
      // Given
      String nchar = "unicode text";

      // When
      SqlParameterType.NCHAR.setParameter(preparedStatement, 1, nchar);

      // Then
      verify(preparedStatement).setNString(1, nchar);
    }

    @Test
    @DisplayName("Should handle NCLOB type")
    void shouldHandleNCLOBType() throws SQLException {
      // Given
      NClob nclob = mock(NClob.class);

      // When
      SqlParameterType.NCLOB.setParameter(preparedStatement, 1, nclob);

      // Then
      verify(preparedStatement).setNClob(1, nclob);
    }

    @Test
    @DisplayName("Should handle ROWID type")
    void shouldHandleROWIDType() throws SQLException {
      // Given
      RowId rowId = mock(RowId.class);

      // When
      SqlParameterType.ROWID.setParameter(preparedStatement, 1, rowId);

      // Then
      verify(preparedStatement).setRowId(1, rowId);
    }

    @Test
    @DisplayName("Should handle ROWID type with non-RowId value")
    void shouldHandleROWIDTypeWithNonRowIdValue() throws SQLException {
      // Given
      String value = "some_rowid_value";

      // When
      SqlParameterType.ROWID.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setObject(1, value);
    }

    @Test
    @DisplayName("Should handle REF type")
    void shouldHandleREFType() throws SQLException {
      // Given
      Ref ref = mock(Ref.class);

      // When
      SqlParameterType.REF.setParameter(preparedStatement, 1, ref);

      // Then
      verify(preparedStatement).setRef(1, ref);
    }

    @Test
    @DisplayName("Should handle REF type with non-Ref value")
    void shouldHandleREFTypeWithNonRefValue() throws SQLException {
      // Given
      String value = "some_ref_value";

      // When
      SqlParameterType.REF.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setObject(1, value);
    }

    @Test
    @DisplayName("Should handle OBJECT type")
    void shouldHandleOBJECTType() throws SQLException {
      // Given
      Object value = new Object();

      // When
      SqlParameterType.OBJECT.setParameter(preparedStatement, 1, value);

      // Then
      verify(preparedStatement).setObject(1, value);
    }
  }

  @Nested
  @DisplayName("Null Handling Tests")
  class NullHandlingTests {
    @ParameterizedTest
    @MethodSource("provideTypesAndSqlTypes")
    @DisplayName("Should handle null values correctly for all types")
    void shouldHandleNullValuesCorrectlyForAllTypes(SqlParameterType type, int expectedSqlType)
        throws SQLException {
      // When
      type.setParameter(preparedStatement, 1, null);

      // Then
      verify(preparedStatement).setNull(1, expectedSqlType);
    }

    private static Stream<Arguments> provideTypesAndSqlTypes() {
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
          Arguments.of(SqlParameterType.NCLOB, Types.NCLOB),
          Arguments.of(SqlParameterType.BYTES, Types.VARBINARY),
          Arguments.of(SqlParameterType.ARRAY, Types.ARRAY),
          Arguments.of(SqlParameterType.URL, Types.DATALINK),
          Arguments.of(SqlParameterType.ROWID, Types.ROWID),
          Arguments.of(SqlParameterType.SQLXML, Types.SQLXML),
          Arguments.of(SqlParameterType.REF, Types.REF),
          Arguments.of(SqlParameterType.UUID, Types.OTHER),
          Arguments.of(SqlParameterType.JSON, Types.OTHER),
          Arguments.of(SqlParameterType.NCHAR, Types.NCHAR));
    }
  }
}
