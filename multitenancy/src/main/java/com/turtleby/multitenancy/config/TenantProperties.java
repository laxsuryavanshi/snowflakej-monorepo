package com.turtleby.multitenancy.config;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Multitenancy configuration properties (prefix: multitenancy). */
@ConfigurationProperties(prefix = "multitenancy")
public class TenantProperties {

  /** HTTP-based tenant resolution configuration. */
  private Http http;

  /** Creates tenant properties with default HTTP configuration. */
  public TenantProperties() {
    this.http = new Http();
  }

  /**
   * Creates tenant properties with the specified HTTP configuration.
   *
   * @param http the HTTP configuration, must not be {@code null}
   */
  public TenantProperties(final Http http) {
    this.http = Objects.requireNonNull(http, "http must not be null");
  }

  /**
   * Returns the HTTP-related configuration.
   *
   * @return the HTTP configuration
   */
  public Http getHttp() {
    return http;
  }

  public void setHttp(Http http) {
    this.http = http;
  }

  /** HTTP resolution options. */
  public static class Http {

    /** Default HTTP header name for tenant identification. */
    private static final String DEFAULT_HEADER_NAME = "X-Tenant-ID";

    private String headerName;

    /** Creates HTTP configuration with the default header name. */
    public Http() {
      this(DEFAULT_HEADER_NAME);
    }

    /**
     * Creates HTTP configuration with the specified header name.
     *
     * @param headerName the HTTP header name to use for tenant identification, must not be blank
     */
    public Http(final String headerName) {
      this.headerName = StringUtils.isBlank(headerName) ? DEFAULT_HEADER_NAME : headerName;
    }

    /**
     * Returns the HTTP header name used for tenant identification.
     *
     * <p>This header should be included in client requests to identify the tenant context for the
     * request.
     *
     * @return the HTTP header name, never blank
     */
    public String getHeaderName() {
      return headerName;
    }

    public void setHeaderName(String headerName) {
      this.headerName = StringUtils.isBlank(headerName) ? DEFAULT_HEADER_NAME : headerName;
    }
  }
}
