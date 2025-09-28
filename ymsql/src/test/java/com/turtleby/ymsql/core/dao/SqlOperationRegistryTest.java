package com.turtleby.ymsql.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.turtleby.ymsql.core.model.SqlOperationSpec;

@DisplayName("SqlOperationRegistry Tests")
class SqlOperationRegistryTest {
  private SqlOperationRegistry emptyRegistry;
  private SqlOperationRegistry populatedRegistry;
  private SqlOperationSpec testSpec;

  @BeforeEach
  void setUp() {
    emptyRegistry = new SqlOperationRegistry(Map.of());

    testSpec =
        SqlOperationSpec.builder()
            .sql("SELECT * FROM users WHERE id = ?")
            .operationType(SqlOperationSpec.SqlOperationType.QUERY)
            .build();

    populatedRegistry = new SqlOperationRegistry(Map.of("getUserById", testSpec));
  }

  @Test
  @DisplayName("Should create registry with empty map")
  void shouldCreateRegistryWithEmptyMap() {
    assertNotNull(emptyRegistry);
    assertTrue(emptyRegistry.isEmpty());
    assertEquals(0, emptyRegistry.size());
  }

  @Test
  @DisplayName("Should create registry with operations")
  void shouldCreateRegistryWithOperations() {
    assertNotNull(populatedRegistry);
    assertFalse(populatedRegistry.isEmpty());
    assertEquals(1, populatedRegistry.size());
  }

  @Test
  @DisplayName("Should retrieve existing operation spec")
  void shouldRetrieveExistingOperationSpec() {
    SqlOperationSpec retrieved = populatedRegistry.getOperationSpec("getUserById");

    assertNotNull(retrieved);
    assertEquals("SELECT * FROM users WHERE id = ?", retrieved.getSql());
    assertEquals(SqlOperationSpec.SqlOperationType.QUERY, retrieved.getOperationType());
  }

  @Test
  @DisplayName("Should throw exception for null operation name")
  void shouldThrowExceptionForNullOperationName() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> populatedRegistry.getOperationSpec(null));

    assertEquals("Operation name cannot be null or empty", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw exception for empty operation name")
  void shouldThrowExceptionForEmptyOperationName() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> populatedRegistry.getOperationSpec(""));

    assertEquals("Operation name cannot be null or empty", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw exception for whitespace-only operation name")
  void shouldThrowExceptionForWhitespaceOnlyOperationName() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> populatedRegistry.getOperationSpec("   "));

    assertEquals("Operation name cannot be null or empty", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw exception for non-existent operation")
  void shouldThrowExceptionForNonExistentOperation() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> populatedRegistry.getOperationSpec("nonExistentOperation"));

    assertEquals(
        "No SqlOperationSpec found with name: nonExistentOperation", exception.getMessage());
  }

  @Test
  @DisplayName("Should correctly identify existing operations")
  void shouldCorrectlyIdentifyExistingOperations() {
    assertTrue(populatedRegistry.hasOperation("getUserById"));
    assertFalse(populatedRegistry.hasOperation("nonExistentOperation"));
    assertFalse(populatedRegistry.hasOperation(null));
    assertFalse(populatedRegistry.hasOperation(""));
  }

  @Test
  @DisplayName("Should return correct size")
  void shouldReturnCorrectSize() {
    assertEquals(0, emptyRegistry.size());
    assertEquals(1, populatedRegistry.size());
  }

  @Test
  @DisplayName("Should correctly identify empty state")
  void shouldCorrectlyIdentifyEmptyState() {
    assertTrue(emptyRegistry.isEmpty());
    assertFalse(populatedRegistry.isEmpty());
  }

  @Test
  @DisplayName("Should be immutable - operations map cannot be modified externally")
  void shouldBeImmutable() {
    Map<String, SqlOperationSpec> originalMap = new HashMap<>();
    originalMap.put("test", testSpec);
    SqlOperationRegistry registry = new SqlOperationRegistry(originalMap);

    // Verify that the registry works correctly
    assertTrue(registry.hasOperation("test"));
    assertEquals(1, registry.size());

    // The internal map should be a copy, so modifications to original should not affect registry
    originalMap.put("newOp", testSpec);
    assertFalse(registry.hasOperation("newOp"));
    assertEquals(1, registry.size());
  }
}
