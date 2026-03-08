package com.turtleby.multitenancy.context;

import org.springframework.lang.Nullable;

import com.turtleby.multitenancy.core.Tenant;

/** Immutable holder of current {@link Tenant}. */
public record TenantContext(@Nullable Tenant tenant) {
  /**
   * Returns the tenant associated with this context.
   *
   * @return the tenant, or {@code null} if no tenant is associated
   */
  @Nullable
  public Tenant getTenant() {
    return tenant;
  }
}
