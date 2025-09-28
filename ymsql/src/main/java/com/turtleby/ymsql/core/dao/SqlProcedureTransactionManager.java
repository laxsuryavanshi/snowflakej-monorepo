package com.turtleby.ymsql.core.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.turtleby.ymsql.core.model.SqlOperationSpec;
import com.turtleby.ymsql.core.model.SqlParameterMode;

/**
 * Manages database transactions for SQL operations.
 *
 * <p>This class handles transaction lifecycle for operations that require special transaction
 * handling, such as procedures with REFCURSOR parameters that need transactions to be disabled.
 */
class SqlProcedureTransactionManager {

  private final Connection connection;
  private final boolean needsTransaction;
  private final boolean originalAutoCommit;

  /**
   * Creates a transaction manager for the given connection and operation.
   *
   * @param connection the database connection
   * @param spec the operation specification
   * @throws SQLException if unable to determine current auto-commit state
   */
  public SqlProcedureTransactionManager(final Connection connection, final SqlOperationSpec spec)
      throws SQLException {
    this.connection = connection;
    this.originalAutoCommit = connection.getAutoCommit();
    this.needsTransaction =
        spec.getParameters().stream()
            .anyMatch(param -> param.getMode() == SqlParameterMode.REFCURSOR);
  }

  /**
   * Begins transaction if needed by disabling auto-commit.
   *
   * @throws SQLException if unable to modify auto-commit setting
   */
  public void beginIfNeeded() throws SQLException {
    if (needsTransaction && originalAutoCommit) {
      connection.setAutoCommit(false);
    }
  }

  /**
   * Commits transaction if it was started by this manager.
   *
   * @throws SQLException if commit fails
   */
  public void commitIfNeeded() throws SQLException {
    if (needsTransaction && originalAutoCommit) {
      connection.commit();
    }
  }

  /**
   * Restores the original auto-commit setting.
   *
   * @throws SQLException if unable to restore auto-commit setting
   */
  public void restoreAutoCommit() throws SQLException {
    if (needsTransaction && originalAutoCommit) {
      try {
        if (!connection.isClosed()) {
          connection.setAutoCommit(true);
        }
      } catch (SQLException e) {
        throw e;
      }
    }
  }

  /**
   * Checks if this transaction manager will manage transactions for the operation.
   *
   * @return true if transaction management is needed
   */
  public boolean isTransactionManaged() {
    return needsTransaction;
  }
}
