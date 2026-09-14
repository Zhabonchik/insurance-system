package org.insurance.insuranceservice.contract.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.application.repository.ApplicationRepository;
import org.insurance.insuranceservice.contract.domain.Contract;
import org.insurance.insuranceservice.contract.domain.ContractStatus;
import org.insurance.insuranceservice.contract.dto.ContractIssueResult;
import org.insurance.insuranceservice.contract.dto.ContractResponse;
import org.insurance.insuranceservice.contract.repository.ContractRepository;
import org.insurance.insuranceservice.error.InvalidApplicationStateException;
import org.insurance.insuranceservice.error.ResourceNotFoundException;
import org.insurance.insuranceservice.outbox.domain.IntegrationOutbox;
import org.insurance.insuranceservice.outbox.domain.OutboxStatus;
import org.insurance.insuranceservice.outbox.repository.IntegrationOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractIssueService {

  private final ApplicationRepository applicationRepository;
  private final ContractRepository contractRepository;
  private final IntegrationOutboxRepository outboxRepository;
  private final ContractNumberGenerator contractNumberGenerator;

  /**
   * Idempotent contract issuance.
   *
   * <p>The application row is locked first, serializing concurrent callers. The {@code
   * UNIQUE(application_id)} constraint is the database backstop.
   */
  @Transactional
  public ContractIssueResult issue(UUID applicationId) {
    Application application =
        applicationRepository
            .findByIdForUpdate(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

    return contractRepository
        .findByApplication_Id(applicationId)
        .map(
            existing -> {
              log.info(
                  "Contract {} already exists for application {}; returning it",
                  existing.getContractNumber(),
                  applicationId);
              return new ContractIssueResult(ContractResponse.from(existing), false);
            })
        .orElseGet(() -> createContract(application));
  }

  private ContractIssueResult createContract(Application application) {
    if (!application.isApproved()) {
      throw new InvalidApplicationStateException(
          "Application %s is in %s state; a contract can only be issued for an APPROVED application"
              .formatted(application.getId(), application.getStatus()));
    }

    Contract contract =
        contractRepository.save(
            Contract.builder()
                .application(application)
                .contractNumber(contractNumberGenerator.nextContractNumber())
                .status(ContractStatus.CREATED)
                .build());

    Instant now = Instant.now();
    // Same transaction as the contract: registry downtime cannot roll this back.
    outboxRepository.save(
        IntegrationOutbox.builder()
            .contract(contract)
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .nextRetryAt(now)
            .build());

    log.info(
        "Issued contract {} for application {}", contract.getContractNumber(), application.getId());
    return new ContractIssueResult(ContractResponse.from(contract), true);
  }
}
