package org.insurance.insuranceservice.contract.dto;

import java.time.Instant;
import java.util.UUID;
import org.insurance.insuranceservice.contract.domain.Contract;
import org.insurance.insuranceservice.contract.domain.ContractStatus;

public record ContractResponse(
    UUID id, UUID applicationId, String contractNumber, ContractStatus status, Instant createdAt) {

  public static ContractResponse from(Contract contract) {
    return new ContractResponse(
        contract.getId(),
        contract.getApplication().getId(),
        contract.getContractNumber(),
        contract.getStatus(),
        contract.getCreatedAt());
  }
}
