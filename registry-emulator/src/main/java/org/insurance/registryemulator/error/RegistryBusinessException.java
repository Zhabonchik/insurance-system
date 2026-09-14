package org.insurance.registryemulator.error;

public class RegistryBusinessException extends RuntimeException {
  public RegistryBusinessException(String message) {
    super(message);
  }
}
