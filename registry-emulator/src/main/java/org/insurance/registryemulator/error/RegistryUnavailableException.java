package org.insurance.registryemulator.error;

public class RegistryUnavailableException extends RuntimeException {
  public RegistryUnavailableException(String message) {
    super(message);
  }
}
