package org.insurance.registryemulator.registry.dto;

/**
 * @param response persisted/returned record
 * @param created true when this call inserted a new row (HTTP 201), false on duplicate (HTTP 200)
 */
public record RegistrationResult(RegistryContractResponse response, boolean created) {}
