package org.insurance.insuranceservice.registry.dto;

import java.math.BigDecimal;
import java.util.UUID;
import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.contract.domain.Contract;

/** Matches the registry emulator's {@code RegistryContractRequest} body. */
public record RegistryRegistrationRequest(
    UUID externalContractId,
    String contractNumber,
    String policyholderName,
    String policyholderPassport,
    BigDecimal insuredAmount,
    Integer termMonths) {

  public static RegistryRegistrationRequest from(Contract contract, Application application) {
    return new RegistryRegistrationRequest(
        contract.getId(),
        contract.getContractNumber(),
        application.getApplicantName(),
        application.getPassportData(),
        application.getInsuredAmount(),
        application.getTermMonths());
  }
}
