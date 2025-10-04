package com.turtleby.multitenancy.context;

import org.springframework.lang.Nullable;

import com.turtleby.multitenancy.core.Tenant;

/** Immutable holder of current {@link Tenant}. */
public class TenantContext {

  private final Tenant tenant;

  /** Empty context. */
  public TenantContext() {
    this(null);
  }

  /** Context with tenant (nullable). */
  public TenantContext(final Tenant tenant) {
    this.tenant = tenant;
  }

  /**
   * Returns the tenant associated with this context.
   *
   * @return the tenant, or {@code null} if no tenant is associated
   */
  @Nullable
  public Tenant getTenant() {
    return tenant;
  }

  /** String representation. */
  @Override
  public String toString() {
    return "TenantContext [tenant=" + tenant + "]";
  }
}
