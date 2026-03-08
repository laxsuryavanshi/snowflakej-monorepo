package com.turtleby.multitenancy.web;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import com.turtleby.multitenancy.context.TenantContext;
import com.turtleby.multitenancy.context.TenantContextHolder;
import com.turtleby.multitenancy.core.Tenant;
import com.turtleby.multitenancy.core.TenantDetailsService;
import com.turtleby.multitenancy.exception.TenantNotFoundException;
import com.turtleby.multitenancy.resolver.TenantResolver;

/** Interceptor resolving tenant, populating context & MDC, then cleaning up post request. */
public class TenantInterceptor implements HandlerInterceptor {

  private final TenantResolver<HttpServletRequest> tenantResolver;
  private final TenantDetailsService tenantDetailsService;

  public TenantInterceptor(
      final TenantResolver<HttpServletRequest> tenantResolver,
      final TenantDetailsService tenantDetailsService) {
    this.tenantResolver = Objects.requireNonNull(tenantResolver, "tenantResolver must not be null");
    this.tenantDetailsService =
        Objects.requireNonNull(tenantDetailsService, "tenantDetailsService must not be null");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Resolves the tenant for the current request and sets up the tenant context. If tenant
   * resolution returns null or fails with TenantNotFoundException, the tenant context will be set
   * with a null tenant. If other errors occur during processing, the exception propagates and the
   * request is not processed.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param handler the request handler
   * @return {@code true} to continue processing, or {@code false} to stop
   * @throws Exception if tenant resolution or loading fails with non-TenantNotFoundException
   */
  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler)
      throws Exception {
    String tenantId = tenantResolver.resolveTenantId(request);
    Tenant tenant = resolveTenant(tenantId);
    TenantContextHolder.setContext(new TenantContext(tenant));
    MDC.put("tenantId", tenantId);
    return true;
  }

  private Tenant resolveTenant(final String tenantId) {
    if (tenantId == null) {
      return null;
    }
    try {
      return tenantDetailsService.getTenantByTenantId(tenantId);
    } catch (TenantNotFoundException e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Cleans up the tenant context and logging MDC after request processing is complete. This
   * method is always called, even if request processing fails.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param handler the request handler
   * @param ex any exception thrown during request processing
   * @throws Exception if cleanup operations fail
   */
  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable Exception ex)
      throws Exception {
    TenantContextHolder.clear();
    MDC.clear();
  }
}
