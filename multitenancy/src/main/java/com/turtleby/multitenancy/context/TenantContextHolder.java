package com.turtleby.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thread-local (inheritable) holder for current {@link TenantContext}. */
public class TenantContextHolder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantContextHolder.class);

  private static final ThreadLocal<TenantContext> CONTEXT = new InheritableThreadLocal<>();

  /** Private constructor to prevent instantiation. */
  private TenantContextHolder() {
    // Utility class
  }

  /** Current context. */
  public static TenantContext getContext() {
    return CONTEXT.get();
  }

  /** Set current context. */
  public static void setContext(final TenantContext tenantContext) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Setting current tenant context to: {}", tenantContext);
    }
    CONTEXT.set(tenantContext);
  }

  /** Clear context. */
  public static void clear() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Clearing current tenant context");
    }
    CONTEXT.remove();
  }
}
