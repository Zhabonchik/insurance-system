package org.insurance.registryemulator.registry.service;

import lombok.RequiredArgsConstructor;
import org.insurance.registryemulator.registry.domain.RegistryRecord;
import org.insurance.registryemulator.registry.dto.RegistryContractRequest;
import org.insurance.registryemulator.registry.repository.RegistryRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * The insert runs in its own transaction so that a unique-constraint violation (concurrent
 * duplicate) cannot mark the caller's transaction rollback-only. The service can then re-read and
 * return the winner.
 */
@Component
@RequiredArgsConstructor
public class RegistryRecordWriter {

  private final RegistryRecordRepository repository;

  @Transactional(propagation = REQUIRES_NEW)
  public RegistryRecord insert(RegistryContractRequest request) {
    RegistryRecord record =
        RegistryRecord.builder()
            .externalContractId(request.externalContractId())
            .contractNumber(request.contractNumber())
            .policyholderName(request.policyholderName())
            .policyholderPassport(request.policyholderPassport())
            .insuredAmount(request.insuredAmount())
            .termMonths(request.termMonths())
            .build();
    // saveAndFlush forces the UNIQUE(external_contract_id) check inside this tx.
    return repository.saveAndFlush(record);
  }
}
