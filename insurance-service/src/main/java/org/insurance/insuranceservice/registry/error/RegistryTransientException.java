package org.insurance.insuranceservice.registry.error;

/** Retryable: 5xx, 408, 429, connection/timeout errors, registry unavailable. */
public class RegistryTransientException extends RuntimeException {

  public RegistryTransientException(String message) {
    super(message);
  }

  public RegistryTransientException(String message, Throwable cause) {
    super(message, cause);
  }
}
