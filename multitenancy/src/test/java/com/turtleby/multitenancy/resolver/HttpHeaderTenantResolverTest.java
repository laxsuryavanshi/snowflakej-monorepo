package com.turtleby.multitenancy.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.turtleby.multitenancy.config.TenantProperties;

/**
 * Unit tests for {@link HttpHeaderTenantResolver}. Tests tenant ID extraction from HTTP headers.
 */
@ExtendWith(MockitoExtension.class)
class HttpHeaderTenantResolverTest {

  @Mock private HttpServletRequest request;

  private HttpHeaderTenantResolver resolver;

  @BeforeEach
  void setUp() {
    TenantProperties properties = new TenantProperties();
    resolver = new HttpHeaderTenantResolver(properties);
  }

  @ParameterizedTest
  @MethodSource("provideTenantIdTestCases")
  void shouldResolveTenantIdFromHeader(
      String headerValue, String expectedTenantId, String testDescription) {
    // Given
    when(request.getHeader("X-Tenant-ID")).thenReturn(headerValue);

    // When
    String tenantId = resolver.resolveTenantId(request);

    // Then
    assertThat(tenantId).isEqualTo(expectedTenantId);
  }

  static Stream<Arguments> provideTenantIdTestCases() {
    return Stream.of(
        Arguments.of("acme-corp", "acme-corp", "valid tenant ID"),
        Arguments.of(null, null, "missing header"),
        Arguments.of("", "", "empty header"),
        Arguments.of("   ", "   ", "blank header"),
        Arguments.of("  acme-corp  ", "  acme-corp  ", "header with whitespace"),
        Arguments.of("tenant-123_abc.example", "tenant-123_abc.example", "special characters"),
        Arguments.of("tenant-café", "tenant-café", "unicode characters"),
        Arguments.of("AcMe-CoRp", "AcMe-CoRp", "mixed case"),
        Arguments.of("a".repeat(1000), "a".repeat(1000), "long tenant ID"));
  }

  @Test
  void shouldResolveFromCustomHeader() {
    // Given
    TenantProperties properties = new TenantProperties(new TenantProperties.Http("Custom-Tenant"));
    HttpHeaderTenantResolver customResolver = new HttpHeaderTenantResolver(properties);

    when(request.getHeader("Custom-Tenant")).thenReturn("custom-tenant-id");

    // When
    String tenantId = customResolver.resolveTenantId(request);

    // Then
    assertThat(tenantId).isEqualTo("custom-tenant-id");
  }

  @Test
  void shouldThrowExceptionWhenRequestIsNull() {
    // The actual implementation doesn't handle null requests gracefully
    // When
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resolver.resolveTenantId(null);
        });
  }
}
