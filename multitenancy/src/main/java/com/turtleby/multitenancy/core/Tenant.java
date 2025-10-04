package com.turtleby.multitenancy.core;

import org.springframework.lang.Nullable;

/** Minimal tenant abstraction (id + schema). */
public interface Tenant {

  /**
   * Returns the unique identifier for this tenant.
   *
   * <p>The tenant ID should be unique across the entire system and is used for tenant resolution
   * and identification in logs and audit trails.
   *
   * @return the tenant identifier, never {@code null}
   */
  String getTenantId();

  /**
   * Returns the isolation mode for this tenant.
   *
   * @return the isolation mode, never {@code null}
   */
  IsolationMode getIsolationMode();

  /**
   * Returns the database schema associated with this tenant.
   *
   * <p>In schema-based isolation mode, this is the name of the PostgreSQL schema. In database-based
   * isolation mode, this may be {@code null} or a placeholder value since each tenant has its own
   * database.
   *
   * @return the database schema name, may be {@code null} in database isolation mode
   */
  @Nullable
  String getSchema();

  /**
   * Returns the database URL associated with this tenant.
   *
   * <p>In database-based isolation mode, this is the name of the JDBC URL. In schema-based
   * schema-based isolation mode, this may be {@code null} or a placeholder value since all tenants
   * share the same database.
   *
   * @return the database URL, may be {@code null} in schema isolation mode
   */
  @Nullable
  String getDatabase();
}
