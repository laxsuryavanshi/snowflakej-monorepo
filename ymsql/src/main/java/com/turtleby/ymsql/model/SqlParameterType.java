package com.turtleby.ymsql.model;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing SQL parameter types with their corresponding aliases and setter methods.
 *
 * <p>This enum provides a type-safe way to set parameters on PreparedStatement objects based on SQL
 * type aliases. Each enum constant represents a specific SQL data type and provides:
 *
 * <ul>
 *   <li>Type aliases for flexible string-to-type mapping
 *   <li>Parameter setting logic for PreparedStatement
 *   <li>Type-specific validation and conversion
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * SqlParameterType type = SqlParameterType.fromString("varchar");
 * type.setParameter(preparedStatement, 1, "Hello World");
 * }</pre>
 */
public enum SqlParameterType {
  STRING("string", "text", "char", "varchar") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.VARCHAR)) {
        return;
      }
      ps.setString(index, value.toString());
    }
  },

  INTEGER("int", "integer", "number") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.INTEGER)) {
        return;
      }
      try {
        ps.setInt(index, Integer.parseInt(value.toString()));
      } catch (NumberFormatException e) {
        throw new SQLException("Invalid integer value: " + value, e);
      }
    }
  },

  LONG("long", "bigint") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.BIGINT)) {
        return;
      }
      try {
        ps.setLong(index, Long.parseLong(value.toString()));
      } catch (NumberFormatException e) {
        throw new SQLException("Invalid long value: " + value, e);
      }
    }
  },

  SHORT("short", "smallint") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.SMALLINT)) {
        return;
      }
      try {
        ps.setShort(index, Short.parseShort(value.toString()));
      } catch (NumberFormatException e) {
        throw new SQLException("Invalid short value: " + value, e);
      }
    }
  },

  BYTE("byte", "tinyint") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.TINYINT)) {
        return;
      }
      try {
        ps.setByte(index, Byte.parseByte(value.toString()));
      } catch (NumberFormatException e) {
        throw new SQLException("Invalid byte value: " + value, e);
      }
    }
  },

  FLOAT("float", "real") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.FLOAT)) {
        return;
      }
      try {
        ps.setFloat(index, Float.parseFloat(value.toString()));
      } catch (NumberFormatException e) {
        throw new SQLException("Invalid float value: " + value, e);
      }
    }
  },

  DOUBLE("double", "double precision", "numeric") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.DOUBLE)) {
        return;
      }
      try {
        ps.setDouble(index, Double.parseDouble(value.toString()));
      } catch (NumberFormatException e) {
        throw new SQLException("Invalid double value: " + value, e);
      }
    }
  },

  DECIMAL("decimal", "money") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.DECIMAL)) {
        return;
      }
      if (value instanceof BigDecimal decimal) {
        ps.setBigDecimal(index, decimal);
      } else {
        try {
          ps.setBigDecimal(index, new BigDecimal(value.toString()));
        } catch (NumberFormatException e) {
          throw new SQLException("Invalid decimal value: " + value, e);
        }
      }
    }
  },

  BOOLEAN("boolean", "bool", "bit") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.BOOLEAN)) {
        return;
      }
      ps.setBoolean(index, Boolean.parseBoolean(value.toString()));
    }
  },

  DATE("date") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.DATE)) {
        return;
      }
      if (value instanceof Date sqlDate) {
        ps.setDate(index, sqlDate);
      } else {
        try {
          ps.setDate(index, Date.valueOf(value.toString()));
        } catch (IllegalArgumentException e) {
          throw new SQLException(
              "Invalid date format: " + value + ". Expected format: yyyy-MM-dd", e);
        }
      }
    }
  },

  TIME("time") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.TIME)) {
        return;
      }
      if (value instanceof Time sqlTime) {
        ps.setTime(index, sqlTime);
      } else {
        try {
          ps.setTime(index, Time.valueOf(value.toString()));
        } catch (IllegalArgumentException e) {
          throw new SQLException(
              "Invalid time format: " + value + ". Expected format: HH:mm:ss", e);
        }
      }
    }
  },

  TIMESTAMP("timestamp", "datetime") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.TIMESTAMP)) {
        return;
      }
      if (value instanceof Timestamp sqlTimestamp) {
        ps.setTimestamp(index, sqlTimestamp);
      } else {
        try {
          ps.setTimestamp(index, Timestamp.valueOf(value.toString()));
        } catch (IllegalArgumentException e) {
          throw new SQLException(
              "Invalid timestamp format: "
                  + value
                  + ". Expected format: yyyy-MM-dd HH:mm:ss[.fffffffff]",
              e);
        }
      }
    }
  },

  BLOB("blob", "longblob", "mediumblob", "tinyblob") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      switch (value) {
        case Blob blob -> ps.setBlob(index, blob);
        case byte[] bytes -> ps.setBlob(index, new ByteArrayInputStream(bytes));
        case null -> ps.setNull(index, Types.BLOB);
        default -> {
          try {
            final byte[] bytes = Base64.getDecoder().decode(value.toString());
            ps.setBlob(index, new ByteArrayInputStream(bytes));
          } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid base64 string for BLOB parameter", e);
          }
        }
      }
    }
  },

  CLOB("clob", "longtext", "mediumtext", "tinytext") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      switch (value) {
        case Clob clob -> ps.setClob(index, clob);
        case null -> ps.setNull(index, Types.CLOB);
        default -> ps.setClob(index, new StringReader(value.toString()));
      }
    }
  },

  NCLOB("nclob", "ntext") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      switch (value) {
        case NClob nclob -> ps.setNClob(index, nclob);
        case null -> ps.setNull(index, Types.NCLOB);
        default -> ps.setNClob(index, new StringReader(value.toString()));
      }
    }
  },

  BYTES("bytes", "varbinary", "binary", "longvarbinary", "image") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      switch (value) {
        case byte[] bytes -> ps.setBytes(index, bytes);
        case null -> ps.setNull(index, Types.VARBINARY);
        default -> {
          final String strValue = value.toString();
          byte[] bytes;
          try {
            bytes = Base64.getDecoder().decode(strValue);
          } catch (IllegalArgumentException e) {
            try {
              bytes = hexStringToByteArray(strValue);
            } catch (SQLException sqlEx) {
              throw new SQLException(
                  "Invalid byte data format. Expected base64 or hex string: " + strValue, sqlEx);
            }
          }
          ps.setBytes(index, bytes);
        }
      }
    }

    private byte[] hexStringToByteArray(String hex) throws SQLException {
      hex = hex.replaceAll("[^0-9A-Fa-f]", "");
      final int len = hex.length();
      if (len == 0) {
        return new byte[0];
      }
      if (len % 2 != 0) {
        throw new SQLException(
            "Invalid hex string length: " + len + ". Hex strings must have even length.");
      }
      final byte[] data = new byte[len / 2];
      try {
        for (int i = 0; i < len; i += 2) {
          data[i / 2] =
              (byte)
                  ((Character.digit(hex.charAt(i), 16) << 4)
                      + Character.digit(hex.charAt(i + 1), 16));
        }
      } catch (NumberFormatException e) {
        throw new SQLException("Invalid hex string: " + hex, e);
      }
      return data;
    }
  },

  ARRAY("array") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.ARRAY)) {
        return;
      }
      if (value instanceof Array sqlArray) {
        ps.setArray(index, sqlArray);
      } else {
        ps.setObject(index, value);
      }
    }
  },

  URL("url", "uri") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.DATALINK)) {
        return;
      }
      if (value instanceof URL url) {
        ps.setURL(index, url);
      } else {
        try {
          final URI uri = URI.create(value.toString());
          ps.setURL(index, uri.toURL());
        } catch (Exception e) {
          throw new SQLException("Invalid URL format: " + value, e);
        }
      }
    }
  },

  ROWID("rowid") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.ROWID)) {
        return;
      }
      if (value instanceof RowId rowId) {
        ps.setRowId(index, rowId);
      } else {
        ps.setObject(index, value);
      }
    }
  },

  SQLXML("xml", "sqlxml") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.SQLXML)) {
        return;
      }
      if (value instanceof SQLXML sqlxml) {
        ps.setSQLXML(index, sqlxml);
      } else {
        final SQLXML sqlxml = ps.getConnection().createSQLXML();
        try {
          sqlxml.setString(value.toString());
          ps.setSQLXML(index, sqlxml);
        } catch (Exception e) {
          try {
            sqlxml.free();
          } catch (SQLException ignored) {
            // Ignore cleanup errors
          }
          throw new SQLException("Failed to set XML value: " + value, e);
        }
      }
    }
  },

  REF("ref", "sqlref") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.REF)) {
        return;
      }
      if (value instanceof Ref ref) {
        ps.setRef(index, ref);
      } else {
        ps.setObject(index, value);
      }
    }
  },

  UUID("uuid") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.OTHER)) {
        return;
      }
      ps.setObject(index, value);
    }
  },

  JSON("json", "jsonb") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.OTHER)) {
        return;
      }
      ps.setObject(index, value.toString());
    }
  },

  NCHAR("nchar", "nvarchar") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      if (setNullIfNull(ps, index, value, Types.NCHAR)) {
        return;
      }
      ps.setNString(index, value.toString());
    }
  },

  OBJECT("object") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      ps.setObject(index, value);
    }
  },

  REFCURSOR("refcursor", "ref_cursor") {
    @Override
    public void setParameter(final PreparedStatement ps, final int index, final Object value)
        throws SQLException {
      throw new SQLException(
          "REFCURSOR is typically used as OUT parameter in "
              + "CallableStatement, not as input in PreparedStatement");
    }

    @Override
    public void setCallableParameter(
        final CallableStatement cs,
        final int index,
        final Object value,
        final SqlParameterMode mode)
        throws SQLException {
      if (mode.isInput()) {
        throw new SQLException("REFCURSOR cannot be used as input parameter");
      }
      registerOutParameter(cs, index);
    }

    @Override
    public void registerOutParameter(final CallableStatement cs, final int index)
        throws SQLException {
      try {
        cs.registerOutParameter(index, Types.REF_CURSOR);
      } catch (SQLException e) {
        // Fallback for databases that don't support REF_CURSOR
        cs.registerOutParameter(index, Types.OTHER);
      }
    }
  };

  private final String[] aliases;

  // Static map for performance optimization
  private static final Map<String, SqlParameterType> ALIAS_MAP = new HashMap<>();

  static {
    for (final SqlParameterType type : values()) {
      for (final String alias : type.aliases) {
        ALIAS_MAP.put(alias.toLowerCase(), type);
      }
    }
  }

  SqlParameterType(final String... aliases) {
    this.aliases = aliases;
  }

  /**
   * Sets a parameter value on a PreparedStatement with appropriate type conversion.
   *
   * @param ps the PreparedStatement to set the parameter on
   * @param index the parameter index (1-based)
   * @param value the value to set
   * @throws SQLException if the parameter cannot be set or if validation fails
   */
  public abstract void setParameter(PreparedStatement ps, int index, Object value)
      throws SQLException;

  /**
   * Sets a parameter value on a CallableStatement with appropriate type conversion and parameter
   * mode handling.
   *
   * @param cs the CallableStatement to set the parameter on
   * @param index the parameter index (1-based)
   * @param value the value to set (can be null for OUT parameters)
   * @param mode the parameter mode (IN, OUT, or INOUT)
   * @throws SQLException if the parameter cannot be set or if validation fails
   */
  public void setCallableParameter(
      final CallableStatement cs, final int index, final Object value, final SqlParameterMode mode)
      throws SQLException {
    switch (mode) {
      case IN -> setParameter(cs, index, value);
      case OUT -> registerOutParameter(cs, index);
      case INOUT -> {
        setParameter(cs, index, value);
        registerOutParameter(cs, index);
      }
      case REFCURSOR -> registerOutParameter(cs, index);
    }
  }

  /**
   * Registers an output parameter on a CallableStatement.
   *
   * @param cs the CallableStatement to register the parameter on
   * @param index the parameter index (1-based)
   * @throws SQLException if the parameter cannot be registered
   */
  public void registerOutParameter(final CallableStatement cs, final int index)
      throws SQLException {
    cs.registerOutParameter(index, getSqlType());
  }

  /**
   * Gets the SQL type for this parameter type.
   *
   * @return the SQL type constant from java.sql.Types
   */
  public int getSqlType() {
    return switch (this) {
      case STRING, NCHAR -> Types.VARCHAR;
      case INTEGER -> Types.INTEGER;
      case LONG -> Types.BIGINT;
      case SHORT -> Types.SMALLINT;
      case BYTE -> Types.TINYINT;
      case FLOAT -> Types.FLOAT;
      case DOUBLE -> Types.DOUBLE;
      case DECIMAL -> Types.DECIMAL;
      case BOOLEAN -> Types.BOOLEAN;
      case DATE -> Types.DATE;
      case TIME -> Types.TIME;
      case TIMESTAMP -> Types.TIMESTAMP;
      case BLOB -> Types.BLOB;
      case CLOB, NCLOB -> Types.CLOB;
      case BYTES -> Types.VARBINARY;
      case ARRAY -> Types.ARRAY;
      case URL -> Types.DATALINK;
      case ROWID -> Types.ROWID;
      case SQLXML -> Types.SQLXML;
      case REF -> Types.REF;
      case REFCURSOR -> Types.REF_CURSOR;
      case UUID, JSON -> Types.OTHER;
      case OBJECT -> Types.JAVA_OBJECT;
    };
  }

  /**
   * Checks if value is null and sets null parameter if so.
   *
   * @param ps the PreparedStatement
   * @param index the parameter index
   * @param value the value to check
   * @param sqlType the SQL type constant from java.sql.Types
   * @return true if value was null and null was set, false otherwise
   */
  protected static boolean setNullIfNull(
      final PreparedStatement ps, final int index, final Object value, final int sqlType)
      throws SQLException {
    if (value == null) {
      ps.setNull(index, sqlType);
      return true;
    }
    return false;
  }

  /**
   * Converts a string representation to a SqlParameterType.
   *
   * @param typeString the string representation of the SQL type (case-insensitive)
   * @return the corresponding SqlParameterType, or OBJECT if not found or if input is null/empty
   */
  public static SqlParameterType fromString(final String typeString) {
    if (typeString == null || typeString.trim().isEmpty()) {
      return OBJECT;
    }

    final String normalizedType = typeString.trim().toLowerCase();
    return ALIAS_MAP.getOrDefault(normalizedType, OBJECT);
  }
}
