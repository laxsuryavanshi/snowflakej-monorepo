package com.turtleby.ymsql.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.turtleby.ymsql.model.SqlOperationSpec;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqlOperationSpecLoader Error Handling Tests")
class SqlOperationSpecLoaderErrorHandlingTest {
  @Mock private ResourceLoader resourceLoader;

  @Mock private Resource resource;

  private SqlOperationSpecLoader loader;

  @BeforeEach
  void setUp() {
    loader = new SqlOperationSpecLoader(resourceLoader);
  }

  @Test
  @DisplayName("Should handle null resource gracefully")
  void shouldHandleNullResourceGracefully() throws IOException {
    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(null);

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle non-existent resource gracefully")
  void shouldHandleNonExistentResourceGracefully() throws IOException {
    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(false);

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should continue processing when individual spec parsing fails")
  void shouldContinueProcessingWhenIndividualSpecParsingFails() throws IOException {
    String yamlContent =
        """
        validOperation:
          sql: "SELECT * FROM users WHERE id = ?"
          operationType: "QUERY"
        invalidOperation:
          sql: "SELECT * FROM users WHERE id = ?"
          # Missing operationType - this should cause parsing to fail
        anotherValidOperation:
          sql: "DELETE FROM users WHERE id = ?"
          operationType: "QUERY"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Should have loaded the valid operations and skipped the invalid one
    assertEquals(2, result.size());
    assertTrue(result.containsKey("validOperation"));
    assertTrue(result.containsKey("anotherValidOperation"));
    assertTrue(result.get("validOperation").isQuery());
    assertTrue(result.get("anotherValidOperation").isQuery());
  }

  @Test
  @DisplayName("Should handle IO exceptions gracefully")
  void shouldHandleIOExceptionsGracefully() throws IOException {
    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenThrow(new IOException("File read error"));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle malformed YAML gracefully")
  void shouldHandleMalformedYamlGracefully() throws IOException {
    String malformedYaml =
        """
        invalidYaml: {
          unclosedBrace: "this yaml is malformed
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(malformedYaml.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle invalid value type for SqlOperationSpec")
  void shouldHandleInvalidValueTypeForSqlOperationSpec() throws IOException {
    String yamlContent =
        """
        validOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
        invalidOperation: "not a map"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Should contain only the valid operation
    assertEquals(1, result.size());
    assertTrue(result.containsKey("validOperation"));
  }

  @Test
  @DisplayName("Should handle empty operationType string")
  void shouldHandleEmptyOperationTypeString() throws IOException {
    String yamlContent =
        """
        invalidOperation:
          sql: "SELECT 1"
          operationType: ""
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle null operationType value")
  void shouldHandleNullOperationTypeValue() throws IOException {
    String yamlContent =
        """
        invalidOperation:
          sql: "SELECT 1"
          operationType: null
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle parameters field that is not a list")
  void shouldHandleParametersFieldNotList() throws IOException {
    String yamlContent =
        """
        invalidOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters: "not a list"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle null parameters object")
  void shouldHandleNullParametersObject() throws IOException {
    String yamlContent =
        """
        invalidOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters: null
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle parameter that is neither string nor map")
  void shouldHandleParameterNeitherStringNorMap() throws IOException {
    String yamlContent =
        """
        invalidOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters:
            - 123
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle null parameter type in map format")
  void shouldHandleNullParameterTypeInMapFormat() throws IOException {
    String yamlContent =
        """
        validOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters:
            - name: "param1"
              type: null
              mode: "in"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // null type gets converted to OBJECT type, so this should succeed
    assertEquals(1, result.size());
    assertTrue(result.containsKey("validOperation"));
  }

  @Test
  @DisplayName("Should handle null parameter mode in map format")
  void shouldHandleNullParameterModeInMapFormat() throws IOException {
    String yamlContent =
        """
        invalidOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters:
            - name: "param1"
              type: "string"
              mode: null
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.getDescription()).thenReturn("classpath:test.yml");

    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    assertTrue(result.isEmpty());
  }
}
