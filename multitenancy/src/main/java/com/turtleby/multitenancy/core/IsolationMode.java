package com.turtleby.multitenancy.core;

/** Tenant isolation modes. */
public enum IsolationMode {
  /** Schema-based isolation mode. Each tenant has its own database schema. */
  SCHEMA("schema"),

  /** Database-based isolation mode. Each tenant has its own separate database. */
  DATABASE("database");

  private final String value;

  IsolationMode(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
