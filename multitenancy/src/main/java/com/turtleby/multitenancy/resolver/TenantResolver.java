package com.turtleby.multitenancy.resolver;

import org.springframework.lang.Nullable;

/** Strategy to extract tenant id from a context (e.g. request). Return null when unavailable. */
@FunctionalInterface
public interface TenantResolver<T> {

  /**
   * Resolves the tenant identifier from the given context.
   *
   * <p>Implementations should extract the tenant identifier from the provided context object. The
   * method should return {@code null} if no tenant identifier can be resolved, allowing the
   * framework to handle the missing tenant scenario appropriately.
   *
   * <h3>Implementation Guidelines:</h3>
   *
   * <ul>
   *   <li>Return {@code null} if tenant cannot be resolved (don't throw exceptions)
   *   <li>Validate and sanitize extracted tenant IDs
   *   <li>Be consistent with tenant ID format and case sensitivity
   *   <li>Consider logging resolution attempts for debugging
   * </ul>
   *
   * @param context the context from which to resolve the tenant identifier, never {@code null}
   * @return the resolved tenant identifier, or {@code null} if not resolvable
   */
  @Nullable
  String resolveTenantId(T context);
}
