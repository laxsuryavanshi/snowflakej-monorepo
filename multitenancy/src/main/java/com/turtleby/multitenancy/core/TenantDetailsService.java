package com.turtleby.multitenancy.core;

import org.springframework.lang.NonNull;

import com.turtleby.multitenancy.exception.TenantNotFoundException;

/**
 * Loads {@link Tenant} by id. Implementations should throw {@link TenantNotFoundException} when
 * absent.
 */
@FunctionalInterface
public interface TenantDetailsService {

  /**
   * Locates and returns the tenant with the given tenant identifier.
   *
   * <p>This method is called by the tenant resolver to resolve tenant information based on the
   * tenant ID extracted from the request context (e.g., HTTP header, JWT claim, subdomain).
   *
   * @param tenantId the tenant identifier to look up, never {@code null}
   * @return the tenant information, never {@code null}
   * @throws TenantNotFoundException if the tenant cannot be found or accessed
   */
  @NonNull
  Tenant getTenantByTenantId(@NonNull String tenantId) throws TenantNotFoundException;
}
