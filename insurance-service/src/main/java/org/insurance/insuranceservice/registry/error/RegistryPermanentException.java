package org.insurance.insuranceservice.registry.error;

/** Non-retryable: validation/business rejection (HTTP 4xx except 408/429). */
public class RegistryPermanentException extends RuntimeException {

  public RegistryPermanentException(String message) {
    super(message);
  }

  public RegistryPermanentException(String message, Throwable cause) {
    super(message, cause);
  }
}
