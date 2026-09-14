package org.insurance.registryemulator.registry.dto;

import org.insurance.registryemulator.registry.domain.RegistryRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegistryContractResponse(
    UUID id,
    UUID externalContractId,
    String contractNumber,
    String policyholderName,
    String policyholderPassport,
    BigDecimal insuredAmount,
    Integer termMonths,
    Instant registeredAt) {

  public static RegistryContractResponse from(RegistryRecord record) {
    return new RegistryContractResponse(
        record.getId(),
        record.getExternalContractId(),
        record.getContractNumber(),
        record.getPolicyholderName(),
        record.getPolicyholderPassport(),
        record.getInsuredAmount(),
        record.getTermMonths(),
        record.getRegisteredAt());
  }
}
