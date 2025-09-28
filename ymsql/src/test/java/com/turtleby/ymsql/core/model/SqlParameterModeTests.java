package com.turtleby.ymsql.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.ParameterMetaData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SqlParameterMode Tests")
public class SqlParameterModeTests {
  @Nested
  @DisplayName("JDBC Mode Mapping Tests")
  class JdbcModeMappingTests {
    @Test
    @DisplayName("Should return correct JDBC mode for IN")
    void shouldReturnCorrectJdbcModeForIn() {
      assertThat(SqlParameterMode.IN.getJdbcMode()).isEqualTo(ParameterMetaData.parameterModeIn);
    }

    @Test
    @DisplayName("Should return correct JDBC mode for OUT")
    void shouldReturnCorrectJdbcModeForOut() {
      assertThat(SqlParameterMode.OUT.getJdbcMode()).isEqualTo(ParameterMetaData.parameterModeOut);
    }

    @Test
    @DisplayName("Should return correct JDBC mode for INOUT")
    void shouldReturnCorrectJdbcModeForInOut() {
      assertThat(SqlParameterMode.INOUT.getJdbcMode())
          .isEqualTo(ParameterMetaData.parameterModeInOut);
    }
  }

  @Nested
  @DisplayName("From JDBC Mode Tests")
  class FromJdbcModeTests {
    @Test
    @DisplayName("Should convert JDBC IN mode correctly")
    void shouldConvertJdbcInModeCorrectly() {
      SqlParameterMode mode = SqlParameterMode.fromJdbcMode(ParameterMetaData.parameterModeIn);
      assertThat(mode).isEqualTo(SqlParameterMode.IN);
    }

    @Test
    @DisplayName("Should convert JDBC OUT mode correctly")
    void shouldConvertJdbcOutModeCorrectly() {
      SqlParameterMode mode = SqlParameterMode.fromJdbcMode(ParameterMetaData.parameterModeOut);
      assertThat(mode).isEqualTo(SqlParameterMode.OUT);
    }

    @Test
    @DisplayName("Should convert JDBC INOUT mode correctly")
    void shouldConvertJdbcInOutModeCorrectly() {
      SqlParameterMode mode = SqlParameterMode.fromJdbcMode(ParameterMetaData.parameterModeInOut);
      assertThat(mode).isEqualTo(SqlParameterMode.INOUT);
    }

    @Test
    @DisplayName("Should throw exception for unknown JDBC mode")
    void shouldThrowExceptionForUnknownJdbcMode() {
      assertThatThrownBy(() -> SqlParameterMode.fromJdbcMode(999))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported parameter mode: 999");
    }
  }

  @Nested
  @DisplayName("From String Tests")
  class FromStringTests {
    @ParameterizedTest
    @CsvSource({
      "IN, IN",
      "in, IN",
      "In, IN",
      "OUT, OUT",
      "out, OUT",
      "Out, OUT",
      "INOUT, INOUT",
      "inout, INOUT",
      "InOut, INOUT",
      "IN_OUT, INOUT",
      "in_out, INOUT"
    })
    @DisplayName("Should convert string values correctly")
    void shouldConvertStringValuesCorrectly(String input, SqlParameterMode expected) {
      SqlParameterMode actual = SqlParameterMode.fromString(input);
      assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException for null")
    void shouldThrowExceptionForNull(String input) {
      assertThatThrownBy(() -> SqlParameterMode.fromString(input))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Parameter mode string must not be null or empty");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Should throw IllegalArgumentException for empty or whitespace strings")
    void shouldThrowExceptionForEmptyOrWhitespaceStrings(String input) {
      assertThatThrownBy(() -> SqlParameterMode.fromString(input))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Parameter mode string must not be null or empty");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unknown string")
    void shouldThrowExceptionForUnknownString() {
      assertThatThrownBy(() -> SqlParameterMode.fromString("UNKNOWN"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown parameter mode: UNKNOWN");
    }

    @Test
    @DisplayName("Should handle strings with extra whitespace")
    void shouldHandleStringsWithExtraWhitespace() {
      SqlParameterMode mode = SqlParameterMode.fromString("  OUT  ");
      assertThat(mode).isEqualTo(SqlParameterMode.OUT);
    }
  }

  @Nested
  @DisplayName("Enum Properties Tests")
  class EnumPropertiesTests {
    @Test
    @DisplayName("Should support valueOf correctly")
    void shouldSupportValueOfCorrectly() {
      assertThat(SqlParameterMode.valueOf("IN")).isEqualTo(SqlParameterMode.IN);
      assertThat(SqlParameterMode.valueOf("OUT")).isEqualTo(SqlParameterMode.OUT);
      assertThat(SqlParameterMode.valueOf("INOUT")).isEqualTo(SqlParameterMode.INOUT);
      assertThat(SqlParameterMode.valueOf("REFCURSOR")).isEqualTo(SqlParameterMode.REFCURSOR);
    }

    @Test
    @DisplayName("Should throw exception for invalid valueOf")
    void shouldThrowExceptionForInvalidValueOf() {
      assertThatThrownBy(() -> SqlParameterMode.valueOf("INVALID"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
