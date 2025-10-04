package com.turtleby.multitenancy.web;

import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link TenantInterceptor} for all requests. */
public class TenantWebMvcConfigurer implements WebMvcConfigurer {

  private final TenantInterceptor tenantInterceptor;

  /** Ctor. */
  public TenantWebMvcConfigurer(final TenantInterceptor tenantInterceptor) {
    this.tenantInterceptor = tenantInterceptor;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Registers the tenant interceptor to process all web requests. The interceptor will be
   * applied to all URL patterns unless overridden by a custom configuration.
   *
   * @param registry the interceptor registry
   */
  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(tenantInterceptor);
  }
}
