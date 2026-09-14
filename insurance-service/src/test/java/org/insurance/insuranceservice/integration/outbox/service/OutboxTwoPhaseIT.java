package org.insurance.insuranceservice.integration.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.insurance.insuranceservice.contract.domain.ContractStatus;
import org.insurance.insuranceservice.contract.service.ContractIssueService;
import org.insurance.insuranceservice.integration.BaseIT;
import org.insurance.insuranceservice.outbox.domain.IntegrationOutbox;
import org.insurance.insuranceservice.outbox.domain.OutboxStatus;
import org.insurance.insuranceservice.outbox.service.OutboxClaimService;
import org.insurance.insuranceservice.outbox.service.OutboxCompletionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OutboxTwoPhaseIT extends BaseIT {

  @Autowired private ContractIssueService contractIssueService;

  @Autowired private OutboxClaimService claimService;

  @Autowired private OutboxCompletionService completionService;

  @Test
  void claimMarksProcessingAndCompletionSucceeds() {
    UUID contractId = issueContract();

    var claimed = claimService.claim(1);

    assertThat(claimed).hasSize(1);
    IntegrationOutbox processing = outboxRepository.findByContract_Id(contractId).orElseThrow();
    assertThat(processing.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
    assertThat(processing.getClaimedAt()).isNotNull();

    completionService.succeed(claimed.getFirst().outboxId());

    assertThat(outboxRepository.findByContract_Id(contractId).orElseThrow().getStatus())
        .isEqualTo(OutboxStatus.SUCCESS);
    assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
        .isEqualTo(ContractStatus.REGISTERED);
  }

  @Test
  void expiredLeaseIsReclaimedAndRetried() {
    UUID contractId = issueContract();
    claimService.claim(1);

    IntegrationOutbox processing = outboxRepository.findByContract_Id(contractId).orElseThrow();
    processing.markProcessing(Instant.now().minus(Duration.ofMinutes(1)));
    outboxRepository.saveAndFlush(processing);

    assertThat(claimService.reclaimStale()).isEqualTo(1);

    IntegrationOutbox reclaimed = outboxRepository.findByContract_Id(contractId).orElseThrow();
    assertThat(reclaimed.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(reclaimed.getRetryCount()).isEqualTo(1);
    assertThat(reclaimed.getNextRetryAt()).isAfter(Instant.now());
  }

  @Test
  void expiredLeaseWithExhaustedRetriesMovesToDeadLetter() {
    UUID contractId = issueContract();
    claimService.claim(1);

    IntegrationOutbox processing = outboxRepository.findByContract_Id(contractId).orElseThrow();
    for (int i = 0; i < 5; i++) {
      processing.recordTransientFailure(Instant.now(), "previous failure");
    }
    processing.markProcessing(Instant.now().minus(Duration.ofMinutes(1)));
    outboxRepository.saveAndFlush(processing);

    claimService.reclaimStale();

    assertThat(outboxRepository.findByContract_Id(contractId).orElseThrow().getStatus())
        .isEqualTo(OutboxStatus.DEAD_LETTER);
  }

  private UUID issueContract() {
    UUID applicationId = persistApprovedApplication().getId();
    return contractIssueService.issue(applicationId).response().id();
  }
}
