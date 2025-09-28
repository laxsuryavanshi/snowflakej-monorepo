package com.turtleby.ymsql.spring.autoconfigure;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymsql")
public record SqlOperationProperties(boolean enabled, List<String> resourcePath) {}
