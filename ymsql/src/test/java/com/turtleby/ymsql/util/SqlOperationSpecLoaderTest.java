package com.turtleby.ymsql.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
import com.turtleby.ymsql.model.SqlOperationSpec.SqlOperationType;
import com.turtleby.ymsql.model.SqlParameter;
import com.turtleby.ymsql.model.SqlParameterMode;
import com.turtleby.ymsql.model.SqlParameterType;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqlOperationSpecLoader Tests")
class SqlOperationSpecLoaderTest {
  @Mock private ResourceLoader resourceLoader;

  @Mock private Resource resource;

  private SqlOperationSpecLoader loader;

  @BeforeEach
  void setUp() {
    loader = new SqlOperationSpecLoader(resourceLoader);
  }

  @Test
  @DisplayName("Should return empty map when no paths provided")
  void shouldReturnEmptyMapWhenNoPathsProvided() {
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(null);
    assertTrue(result.isEmpty());

    result = loader.loadFromPaths(List.of());
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should load operations from single valid YAML resource")
  void shouldLoadOperationsFromSingleValidYamlResource() throws IOException {
    // Arrange
    String yamlContent =
        """
        getUserById:
          sql: "SELECT * FROM users WHERE id = ?"
          operationType: "QUERY"
          parameters: ["integer"]
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.containsKey("getUserById"));

    SqlOperationSpec spec = result.get("getUserById");
    assertEquals("SELECT * FROM users WHERE id = ?", spec.getSql());
    assertEquals(SqlOperationType.QUERY, spec.getOperationType());
    assertEquals(1, spec.getParameters().size());

    SqlParameter param = spec.getParameters().get(0);
    assertEquals(SqlParameterType.INTEGER, param.getType());
    assertEquals(SqlParameterMode.IN, param.getMode());
  }

  @Test
  @DisplayName("Should parse parameter string formats correctly")
  void shouldParseParameterStringFormatsCorrectly() throws IOException {
    // Arrange
    String yamlContent =
        """
        testOperation:
          sql: "CALL test_proc(?, ?, ?)"
          operationType: "PROCEDURE"
          parameters:
            - "integer:in"
            - "string:out"
            - "user_cursor:refcursor:refcursor"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertEquals(1, result.size());
    SqlOperationSpec spec = result.get("testOperation");
    assertEquals(3, spec.getParameters().size());

    // Test IN parameter
    SqlParameter param1 = spec.getParameters().get(0);
    assertNull(param1.getName());
    assertEquals(SqlParameterType.INTEGER, param1.getType());
    assertEquals(SqlParameterMode.IN, param1.getMode());

    // Test OUT parameter
    SqlParameter param2 = spec.getParameters().get(1);
    assertNull(param2.getName());
    assertEquals(SqlParameterType.STRING, param2.getType());
    assertEquals(SqlParameterMode.OUT, param2.getMode());

    // Test named REFCURSOR parameter
    SqlParameter param3 = spec.getParameters().get(2);
    assertEquals("user_cursor", param3.getName());
    assertEquals(SqlParameterType.REFCURSOR, param3.getType());
    assertEquals(SqlParameterMode.REFCURSOR, param3.getMode());
  }

  @Test
  @DisplayName("Should parse parameter map formats correctly")
  void shouldParseParameterMapFormatsCorrectly() throws IOException {
    // Arrange
    String yamlContent =
        """
        testOperation:
          sql: "CALL test_proc(?, ?)"
          operationType: "PROCEDURE"
          parameters:
            - name: "user_id"
              type: "integer"
              mode: "in"
            - name: "result_cursor"
              type: "refcursor"
              mode: "refcursor"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertEquals(1, result.size());
    SqlOperationSpec spec = result.get("testOperation");
    assertEquals(2, spec.getParameters().size());

    // Test IN parameter with name
    SqlParameter param1 = spec.getParameters().get(0);
    assertEquals("user_id", param1.getName());
    assertEquals(SqlParameterType.INTEGER, param1.getType());
    assertEquals(SqlParameterMode.IN, param1.getMode());

    // Test REFCURSOR parameter with name
    SqlParameter param2 = spec.getParameters().get(1);
    assertEquals("result_cursor", param2.getName());
    assertEquals(SqlParameterType.REFCURSOR, param2.getType());
    assertEquals(SqlParameterMode.REFCURSOR, param2.getMode());
  }

  @Test
  @DisplayName("Should handle mixed parameter formats")
  void shouldHandleMixedParameterFormats() throws IOException {
    // Arrange
    String yamlContent =
        """
        testOperation:
          sql: "CALL test_proc(?, ?, ?)"
          operationType: "PROCEDURE"
          parameters:
            - "integer:in"
            - name: "output_value"
              type: "string"
              mode: "out"
            - "decimal:inout"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertEquals(1, result.size());
    SqlOperationSpec spec = result.get("testOperation");
    assertEquals(3, spec.getParameters().size());

    // String format
    SqlParameter param1 = spec.getParameters().get(0);
    assertEquals(SqlParameterMode.IN, param1.getMode());
    assertEquals(SqlParameterType.INTEGER, param1.getType());

    // Map format
    SqlParameter param2 = spec.getParameters().get(1);
    assertEquals("output_value", param2.getName());
    assertEquals(SqlParameterMode.OUT, param2.getMode());
    assertEquals(SqlParameterType.STRING, param2.getType());

    // String format with INOUT
    SqlParameter param3 = spec.getParameters().get(2);
    assertEquals(SqlParameterMode.INOUT, param3.getMode());
    assertEquals(SqlParameterType.DECIMAL, param3.getType());
  }

  @Test
  @DisplayName("Should handle invalid parameter string format")
  void shouldHandleInvalidParameterStringFormat() throws IOException {
    // Arrange
    String yamlContent =
        """
        testOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters: ["invalid:format:with:too:many:parts"]
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert - should return empty map due to parsing error
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle invalid parameter mode")
  void shouldHandleInvalidParameterMode() throws IOException {
    // Arrange
    String yamlContent =
        """
        testOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters: ["integer:invalid_mode"]
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert - should return empty map due to parsing error
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle missing type in map format")
  void shouldHandleMissingTypeInMapFormat() throws IOException {
    // Arrange
    String yamlContent =
        """
        testOperation:
          sql: "SELECT 1"
          operationType: "QUERY"
          parameters:
            - name: "param1"
              mode: "in"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert - should return empty map due to missing type
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle duplicate operation names across resources")
  void shouldHandleDuplicateOperationNamesAcrossResources() throws IOException {
    // Arrange
    String yamlContent1 =
        """
        operation1:
          sql: "SELECT 1"
          operationType: "QUERY"
        """;

    String yamlContent2 =
        """
        operation1:
          sql: "SELECT 2"
          operationType: "QUERY"
        """;

    Resource resource1 = mock(Resource.class);
    Resource resource2 = mock(Resource.class);

    when(resourceLoader.getResource("classpath:test1.yml")).thenReturn(resource1);
    when(resourceLoader.getResource("classpath:test2.yml")).thenReturn(resource2);
    when(resource1.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent1.getBytes()));
    when(resource2.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent2.getBytes()));
    when(resource1.exists()).thenReturn(true);
    when(resource2.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test1.yml", "test2.yml"));

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.containsKey("operation1"));

    SqlOperationSpec spec = result.get("operation1");
    assertNotNull(spec.getSql());
    // The SQL should be either "SELECT 1" or "SELECT 2" depending on which was
    // processed first
    assertTrue(spec.getSql().equals("SELECT 1") || spec.getSql().equals("SELECT 2"));
  }

  @Test
  @DisplayName("Should handle duplicate operation names within single resource")
  void shouldHandleDuplicateOperationNamesWithinSingleResource() throws IOException {
    // Arrange
    String yamlContent =
        """
        operation1:
          sql: "SELECT 1"
          operationType: "QUERY"
        operation1:
          sql: "SELECT 2"
          operationType: "PROCEDURE"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    // YAML parser will only keep the last occurrence when parsing a single resource
    assertEquals(1, result.size());
    assertTrue(result.containsKey("operation1"));

    SqlOperationSpec spec = result.get("operation1");
    assertEquals("SELECT 2", spec.getSql()); // Last occurrence wins in YAML
    assertEquals(SqlOperationType.PROCEDURE, spec.getOperationType());
  }

  @Test
  @DisplayName("Should handle resource loading failures gracefully")
  void shouldHandleResourceLoadingFailuresGracefully() throws IOException {
    // Arrange
    when(resourceLoader.getResource("classpath:nonexistent.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(new IOException("Resource not found"));
    // This line ensures that the resource is considered to exist, but reading it fails
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("nonexistent.yml"));

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle resources with protocol prefix")
  void shouldHandleResourcesWithProtocolPrefix() throws IOException {
    // Arrange
    String yamlContent =
        """
        testOp:
          sql: "SELECT 1"
          operationType: "QUERY"
        """;

    when(resourceLoader.getResource("file:/path/to/test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("file:/path/to/test.yml"));

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.containsKey("testOp"));
  }

  @Test
  @DisplayName("Should parse minimal valid operation spec")
  void shouldParseMinimalValidOperationSpec() throws IOException {
    // Arrange
    String yamlContent =
        """
        simpleQuery:
          sql: "SELECT 1"
          operationType: "QUERY"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertEquals(1, result.size());
    SqlOperationSpec spec = result.get("simpleQuery");
    assertEquals("SELECT 1", spec.getSql());
    assertEquals(SqlOperationType.QUERY, spec.getOperationType());
    assertNull(spec.getParameters());
  }

  @Test
  @DisplayName("Should return empty map for missing SQL field")
  void shouldReturnEmptyMapForMissingSqlField() throws IOException {
    // Arrange
    String yamlContent =
        """
        invalidSpec:
          operationType: "QUERY"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return empty map for missing operation type")
  void shouldReturnEmptyMapForMissingOperationType() throws IOException {
    // Arrange
    String yamlContent =
        """
        invalidSpec:
          sql: "SELECT 1"
        """;
    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return empty map for invalid operation type")
  void shouldReturnEmptyMapForInvalidOperationType() throws IOException {
    // Arrange
    String yamlContent =
        """
        invalidSpec:
          sql: "SELECT 1"
          operationType: "INVALID_TYPE"
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should parse parameter types correctly")
  void shouldParseParametersCorrectly() throws IOException {
    // Arrange
    String yamlContent =
        """
        queryWithParams:
          sql: "SELECT * FROM users WHERE id = ? AND name = ?"
          operationType: "QUERY"
          parameters: ["integer", "string"]
        """;

    when(resourceLoader.getResource("classpath:test.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("test.yml"));

    // Assert
    assertEquals(1, result.size());
    SqlOperationSpec spec = result.get("queryWithParams");
    assertNotNull(spec.getParameters());
    assertEquals(2, spec.getParameters().size());

    SqlParameter param1 = spec.getParameters().get(0);
    assertEquals(SqlParameterType.INTEGER, param1.getType());
    assertEquals(SqlParameterMode.IN, param1.getMode());

    SqlParameter param2 = spec.getParameters().get(1);
    assertEquals(SqlParameterType.STRING, param2.getType());
    assertEquals(SqlParameterMode.IN, param2.getMode());
  }

  @Test
  @DisplayName("Should handle empty YAML content gracefully")
  void shouldHandleEmptyYamlContentGracefully() throws IOException {
    // Arrange
    when(resourceLoader.getResource("classpath:empty.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("empty.yml"));

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle malformed YAML gracefully")
  void shouldHandleMalformedYamlGracefully() throws IOException {
    // Arrange
    String malformedYaml = "invalid: yaml: content: [unclosed";
    when(resourceLoader.getResource("classpath:malformed.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(malformedYaml.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("malformed.yml"));

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle multiple operations in single resource")
  void shouldHandleMultipleOperationsInSingleResource() throws IOException {
    // Arrange
    String yamlContent =
        """
        operation1:
          sql: "SELECT 1"
          operationType: "QUERY"
        operation2:
          sql: "SELECT 2"
          operationType: "PROCEDURE"
          parameters: ["string"]
        """;

    when(resourceLoader.getResource("classpath:multi.yml")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));
    when(resource.exists()).thenReturn(true);

    // Act
    Map<String, SqlOperationSpec> result = loader.loadFromPaths(List.of("multi.yml"));

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.containsKey("operation1"));
    assertTrue(result.containsKey("operation2"));

    SqlOperationSpec spec1 = result.get("operation1");
    assertEquals("SELECT 1", spec1.getSql());
    assertEquals(SqlOperationType.QUERY, spec1.getOperationType());

    SqlOperationSpec spec2 = result.get("operation2");
    assertEquals("SELECT 2", spec2.getSql());
    assertEquals(SqlOperationType.PROCEDURE, spec2.getOperationType());
    assertEquals(1, spec2.getParameters().size());

    SqlParameter param = spec2.getParameters().get(0);
    assertEquals(SqlParameterType.STRING, param.getType());
    assertEquals(SqlParameterMode.IN, param.getMode());
  }
}
