package org.insurance.insuranceservice.contract.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.insurance.insuranceservice.contract.domain.ContractNumberSequence;
import org.insurance.insuranceservice.contract.repository.ContractNumberSequenceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ContractNumberGenerator {

  static final int SEQUENCE_ID = 1;
  private static final String NUMBER_TEMPLATE = "LIFE-%d-%06d";

  private final ContractNumberSequenceRepository sequenceRepository;

  /**
   * Must run inside the contract-issuance transaction ({@code MANDATORY}); the single sequence row
   * is locked so concurrent issuers receive distinct numbers.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public String nextContractNumber() {
    ContractNumberSequence sequence =
        sequenceRepository
            .findForUpdate(SEQUENCE_ID)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "contract_number_sequence row id=%d is missing; Liquibase seed did not run"
                            .formatted(SEQUENCE_ID)));
    long value = sequence.next();
    int year = LocalDate.now(ZoneOffset.UTC).getYear();
    return NUMBER_TEMPLATE.formatted(year, value);
  }
}
