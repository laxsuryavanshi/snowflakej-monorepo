package com.turtleby.ymsql.spring.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import com.turtleby.ymsql.core.model.SqlOperationSpec;
import com.turtleby.ymsql.core.model.SqlParameter;
import com.turtleby.ymsql.core.model.SqlParameterMode;
import com.turtleby.ymsql.core.model.SqlParameterType;

/**
 * Utility class responsible for loading and parsing SQL operation specifications from YAML
 * resources. This class handles the conversion of YAML configuration files into SqlOperationSpec
 * objects.
 *
 * <p>The loader supports parallel loading of multiple resource paths for improved performance and
 * provides comprehensive error handling and logging.
 */
public class SqlOperationSpecLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlOperationSpecLoader.class);

  private final ResourceLoader resourceLoader;

  /**
   * Creates a new SqlOperationSpecLoader with the specified ResourceLoader.
   *
   * @param resourceLoader the Spring ResourceLoader to use for loading resources
   */
  public SqlOperationSpecLoader(final ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * Loads SQL operation specifications from multiple resource paths.
   *
   * @param resourcePaths list of resource paths to load from
   * @return map of operation name to SqlOperationSpec
   */
  public Map<String, SqlOperationSpec> loadFromPaths(final List<String> resourcePaths) {
    if (resourcePaths == null || resourcePaths.isEmpty()) {
      LOGGER.warn("No resource paths provided for SqlOperationSpecs");
      return Collections.emptyMap();
    }

    final long startTime = System.currentTimeMillis();
    final Map<String, SqlOperationSpec> operations = new ConcurrentHashMap<>();

    resourcePaths.parallelStream()
        .map(this::getResourceFromLocation)
        .map(this::loadSqlOperationSpecs)
        .flatMap(ops -> ops.entrySet().stream())
        .forEach(
            entry -> {
              final String key = entry.getKey();
              final SqlOperationSpec existing = operations.putIfAbsent(key, entry.getValue());
              if (existing != null) {
                LOGGER.warn("Ignoring duplicate SqlOperationSpec name '{}'", key);
              }
            });

    final long loadTime = System.currentTimeMillis() - startTime;
    LOGGER.info(
        "Loaded {} SqlOperationSpecs from {} resources in {}ms",
        operations.size(),
        resourcePaths.size(),
        loadTime);

    return operations;
  }

  private Resource getResourceFromLocation(final String location) {
    return location.indexOf(":") >= 0
        ? resourceLoader.getResource(location)
        : resourceLoader.getResource(ResourceUtils.CLASSPATH_URL_PREFIX + location);
  }

  private Map<String, SqlOperationSpec> loadSqlOperationSpecs(final Resource resource) {
    if (resource == null || !resource.exists()) {
      LOGGER.warn("Resource is null or does not exist: {}", resource);
      return Collections.emptyMap();
    }

    try (var stream = resource.getInputStream()) {
      final Yaml yaml = new Yaml();
      final Map<String, Object> rawData = yaml.loadAs(stream, Map.class);

      if (rawData == null || rawData.isEmpty()) {
        LOGGER.warn(
            "Resource '{}' contains no SqlOperationSpec definitions", resource.getDescription());
        return Collections.emptyMap();
      }

      final Map<String, SqlOperationSpec> result = new HashMap<>(rawData.size());
      for (final Map.Entry<String, Object> entry : rawData.entrySet()) {
        final String key = entry.getKey();
        final Object value = entry.getValue();
        try {
          if (value instanceof final Map<?, ?> valueMap) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> typedMap = (Map<String, Object>) valueMap;
            final SqlOperationSpec spec = parseSqlOperationSpec(typedMap, key);
            result.put(key, spec);
          } else {
            final String valueType = value != null ? value.getClass().getSimpleName() : "null";
            throw new IllegalArgumentException(
                "Invalid value type for SqlOperationSpec '"
                    + key
                    + "' in resource '"
                    + resource.getDescription()
                    + "'. Expected Map but got: "
                    + valueType);
          }
        } catch (final Exception ex) {
          LOGGER.error(
              "Failed to parse SqlOperationSpec '{}' from resource: {}",
              key,
              resource.getDescription(),
              ex);
        }
      }

      return result;
    } catch (final IOException ex) {
      LOGGER.error(
          "Failed to load SqlOperationSpec definitions from resource: {}",
          resource.getDescription(),
          ex);
      return Collections.emptyMap();
    } catch (final Exception ex) {
      LOGGER.error(
          "Unexpected error while parsing YAML from resource: {}", resource.getDescription(), ex);
      return Collections.emptyMap();
    }
  }

  private SqlOperationSpec parseSqlOperationSpec(
      final Map<String, Object> value, final String key) {
    final SqlOperationSpec.SqlOperationSpecBuilder builder = SqlOperationSpec.builder();

    // Validate and set SQL
    if (!value.containsKey("sql")
        || value.get("sql") == null
        || ((String) value.get("sql")).trim().isEmpty()) {
      throw new IllegalArgumentException(
          "SQL field is missing or empty for operation specification '" + key + "'");
    } else {
      builder.sql(((String) value.get("sql")).trim());
    }

    // Parse operation type
    if (value.containsKey("operationType")) {
      final String opTypeStr = (String) value.get("operationType");
      if (opTypeStr != null && !opTypeStr.trim().isEmpty()) {
        try {
          final SqlOperationSpec.SqlOperationType opType =
              SqlOperationSpec.SqlOperationType.valueOf(opTypeStr.trim().toUpperCase());
          builder.operationType(opType);
        } catch (final IllegalArgumentException ex) {
          throw new IllegalArgumentException(
              "Invalid operationType '"
                  + opTypeStr
                  + "' found for operation specification '"
                  + key
                  + "'. Valid types are: "
                  + Arrays.toString(SqlOperationSpec.SqlOperationType.values()),
              ex);
        }
      }
    } else {
      throw new IllegalArgumentException(
          "operationType field is missing for operation specification '" + key + "'");
    }

    if (value.containsKey("parameters")) {
      final Object parametersObj = value.get("parameters");
      if (parametersObj instanceof final List<?> parametersList) {
        final List<SqlParameter> parsedParameters =
            parametersList.stream().map(this::parseParameterSpec).toList();
        builder.parameters(parsedParameters);
      } else {
        throw new IllegalArgumentException(
            "Parameters field is not a list. Expected List but got: "
                + (parametersObj != null ? parametersObj.getClass().getSimpleName() : "null"));
      }
    }

    return builder.build();
  }

  private SqlParameter parseParameterSpec(final Object paramObj) {
    if (paramObj instanceof final String paramStr) {
      return SqlParameter.fromString(paramStr);
    }
    if (paramObj instanceof final Map<?, ?> paramMap) {
      @SuppressWarnings("unchecked")
      final Map<String, String> typedMap = (Map<String, String>) paramMap;
      return parseParameterSpec(typedMap);
    }
    throw new IllegalArgumentException(
        "Parameter specification must be a string or map, got: "
            + (paramObj != null ? paramObj.getClass().getSimpleName() : "null"));
  }

  private SqlParameter parseParameterSpec(final Map<String, String> paramMap) {
    final SqlParameter.SqlParameterBuilder builder = SqlParameter.builder();

    // Parse parameter type (required)
    if (paramMap.containsKey("type")) {
      final String typeStr = paramMap.get("type");
      final SqlParameterType type = SqlParameterType.fromString(typeStr);
      builder.type(type);
    } else {
      throw new IllegalArgumentException("Parameter specification missing required 'type' field");
    }

    // Parse parameter mode (optional)
    if (paramMap.containsKey("mode")) {
      final String modeStr = paramMap.get("mode");
      final SqlParameterMode mode = SqlParameterMode.fromString(modeStr);
      builder.mode(mode);
    }

    // Parse parameter name (optional)
    if (paramMap.containsKey("name")) {
      builder.name(paramMap.get("name"));
    }

    return builder.build();
  }
}
