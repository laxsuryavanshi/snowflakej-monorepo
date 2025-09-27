package com.turtleby.ymsql.autoconfigure;

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import com.turtleby.ymsql.dao.SqlOperationRegistry;
import com.turtleby.ymsql.model.SqlOperationSpec;
import com.turtleby.ymsql.util.SqlOperationSpecLoader;

@ConditionalOnProperty(
    prefix = "ymsql",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@AutoConfiguration
@EnableConfigurationProperties(SqlOperationProperties.class)
public class YmsqlAutoConfiguration {
  @Bean
  SqlOperationRegistry sqlOperationRegistry(
      final ResourceLoader resourceLoader, final SqlOperationProperties properties) {
    final SqlOperationSpecLoader loader = new SqlOperationSpecLoader(resourceLoader);
    final Map<String, SqlOperationSpec> operations =
        loader.loadFromPaths(properties.resourcePath());
    return new SqlOperationRegistry(operations);
  }
}
