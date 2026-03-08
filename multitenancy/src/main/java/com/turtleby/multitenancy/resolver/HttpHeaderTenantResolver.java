package com.turtleby.multitenancy.resolver;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.turtleby.multitenancy.configuration.TenantProperties;

/** Resolves tenant id from configured HTTP header. */
public class HttpHeaderTenantResolver implements TenantResolver<HttpServletRequest> {

  private final TenantProperties tenantProperties;

  /**
   * Creates a new HTTP header tenant resolver with the specified properties.
   *
   * @param tenantProperties the tenant configuration properties, must not be {@code null}
   */
  public HttpHeaderTenantResolver(final TenantProperties tenantProperties) {
    this.tenantProperties =
        Objects.requireNonNull(tenantProperties, "tenantProperties must not be null");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Extracts the tenant identifier from the HTTP header specified in the tenant properties
   * configuration.
   *
   * @param request the HTTP servlet request
   * @return the tenant identifier from the header, or {@code null} if header is not present
   * @throws IllegalArgumentException if the request is {@code null}
   */
  @Nullable
  @Override
  public String resolveTenantId(final HttpServletRequest request) {
    Assert.notNull(request, "HttpServletRequest must not be null");

    return request.getHeader(tenantProperties.getHttp().getHeaderName());
  }
}
