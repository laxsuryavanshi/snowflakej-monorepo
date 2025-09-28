package com.turtleby.ymsql.spring.autoconfigure;

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import com.turtleby.ymsql.core.dao.SqlOperationRegistry;
import com.turtleby.ymsql.core.model.SqlOperationSpec;
import com.turtleby.ymsql.spring.util.SqlOperationSpecLoader;

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
