package com.turtleby.multitenancy.exception;

/** Thrown when a tenant id cannot be resolved to a valid tenant. */
public class TenantNotFoundException extends RuntimeException {

  /** Default ctor. */
  public TenantNotFoundException() {
    super();
  }

  /** Message ctor. */
  public TenantNotFoundException(String message) {
    super(message);
  }

  /** Message + cause ctor. */
  public TenantNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Cause ctor. */
  public TenantNotFoundException(Throwable cause) {
    super(cause);
  }
}
