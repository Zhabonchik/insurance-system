package org.insurance.registryemulator.error;

import java.util.UUID;

public class RegistryRecordNotFoundException extends RuntimeException {
  public RegistryRecordNotFoundException(UUID externalContractId) {
    super("Registry record not found for externalContractId=" + externalContractId);
  }
}
