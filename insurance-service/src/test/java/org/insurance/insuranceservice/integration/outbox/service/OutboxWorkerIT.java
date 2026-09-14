package org.insurance.insuranceservice.integration.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.UUID;
import org.insurance.insuranceservice.contract.domain.ContractStatus;
import org.insurance.insuranceservice.contract.service.ContractIssueService;
import org.insurance.insuranceservice.integration.BaseIT;
import org.insurance.insuranceservice.outbox.domain.IntegrationOutbox;
import org.insurance.insuranceservice.outbox.domain.OutboxStatus;
import org.insurance.insuranceservice.outbox.service.OutboxWorker;
import org.insurance.insuranceservice.registry.client.RegistryClient;
import org.insurance.insuranceservice.registry.error.RegistryPermanentException;
import org.insurance.insuranceservice.registry.error.RegistryTransientException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class OutboxWorkerIT extends BaseIT {

  @Autowired private ContractIssueService contractIssueService;

  @Autowired private OutboxWorker outboxWorker;

  @MockitoBean private RegistryClient registryClient;

  @Test
  void contractSurvivesRegistryOutageAndIsRegisteredAfterRecovery() {
    UUID applicationId = persistApprovedApplication().getId();
    UUID contractId = contractIssueService.issue(applicationId).response().id();

    doThrow(new RegistryTransientException("registry down")).when(registryClient).register(any());

    assertThat(outboxWorker.processNext()).isTrue();

    IntegrationOutbox afterFailure = outboxRepository.findByContract_Id(contractId).orElseThrow();
    assertThat(afterFailure.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(afterFailure.getRetryCount()).isEqualTo(1);
    assertThat(afterFailure.getNextRetryAt()).isAfter(Instant.now().minusSeconds(1));
    assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
        .isEqualTo(ContractStatus.CREATED);

    // Simulate that the backoff window has elapsed.
    afterFailure.scheduleRetry(Instant.now().minusSeconds(5));
    outboxRepository.saveAndFlush(afterFailure);

    doNothing().when(registryClient).register(any());

    assertThat(outboxWorker.processNext()).isTrue();

    assertThat(outboxRepository.findByContract_Id(contractId).orElseThrow().getStatus())
        .isEqualTo(OutboxStatus.SUCCESS);
    assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
        .isEqualTo(ContractStatus.REGISTERED);
  }

  @Test
  void permanentRegistryErrorStopsRetries() {
    UUID applicationId = persistApprovedApplication().getId();
    UUID contractId = contractIssueService.issue(applicationId).response().id();

    doThrow(new RegistryPermanentException("invalid passport"))
        .when(registryClient)
        .register(any());

    assertThat(outboxWorker.processNext()).isTrue();

    IntegrationOutbox outbox = outboxRepository.findByContract_Id(contractId).orElseThrow();
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED_BUSINESS);
    assertThat(outbox.getRetryCount()).isZero();
    assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
        .isEqualTo(ContractStatus.CREATED);
  }
}
