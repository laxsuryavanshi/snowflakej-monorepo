package com.turtleby.ymsql.core.dao;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.turtleby.ymsql.core.model.SqlOperationSpec;

/**
 * Registry for SQL operation specifications.
 *
 * <p>This class provides a centralized repository for looking up SqlOperationSpec instances by
 * name.
 *
 * <p>The registry is immutable after construction and thread-safe.
 */
public class SqlOperationRegistry {
  private final Map<String, SqlOperationSpec> operations;

  /**
   * Creates a new SqlOperationRegistry with the provided operations map. The map is copied to
   * ensure immutability.
   *
   * @param operations the map of operation name to SqlOperationSpec
   */
  public SqlOperationRegistry(final Map<String, SqlOperationSpec> operations) {
    this.operations = Map.copyOf(operations);
  }

  /**
   * Retrieves a SqlOperationSpec by its name.
   *
   * @param name the name of the operation specification to retrieve
   * @return the SqlOperationSpec associated with the name
   * @throws IllegalArgumentException if the name is null/empty or not found
   */
  public SqlOperationSpec getOperationSpec(final String name) throws IllegalArgumentException {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("Operation name cannot be null or empty");
    }

    if (!operations.containsKey(name)) {
      throw new IllegalArgumentException("No SqlOperationSpec found with name: " + name);
    }

    return operations.get(name);
  }

  /**
   * Checks if an operation with the given name exists in the registry.
   *
   * @param name the name of the operation to check
   * @return true if the operation exists, false otherwise
   */
  public boolean hasOperation(final String name) {
    return name != null && operations.containsKey(name);
  }

  /**
   * Returns the number of registered operations.
   *
   * @return the count of operations in the registry
   */
  public int size() {
    return operations.size();
  }

  /**
   * Checks if the registry is empty.
   *
   * @return true if no operations are registered, false otherwise
   */
  public boolean isEmpty() {
    return operations.isEmpty();
  }
}
