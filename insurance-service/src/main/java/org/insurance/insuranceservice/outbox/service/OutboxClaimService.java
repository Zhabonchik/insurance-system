package org.insurance.insuranceservice.outbox.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.insuranceservice.contract.domain.Contract;
import org.insurance.insuranceservice.outbox.domain.IntegrationOutbox;
import org.insurance.insuranceservice.outbox.domain.OutboxStatus;
import org.insurance.insuranceservice.outbox.repository.IntegrationOutboxRepository;
import org.insurance.insuranceservice.registry.dto.RegistryRegistrationRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * First phase of outbox processing. Marks due rows as {@code PROCESSING} and returns everything the
 * registry call needs, so no database lock is held while talking to the registry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxClaimService {

  private final IntegrationOutboxRepository outboxRepository;
  private final OutboxCompletionService completionService;
  private final OutboxProperties properties;

  @Transactional
  public List<ClaimedRegistration> claim(int limit) {
    Instant claimedAt = Instant.now();
    List<IntegrationOutbox> due =
        outboxRepository.findDueForProcessing(
            OutboxStatus.PENDING, claimedAt, PageRequest.of(0, limit));

    List<ClaimedRegistration> claimed = new ArrayList<>(due.size());
    for (IntegrationOutbox outbox : due) {
      Contract contract = outbox.getContract();
      claimed.add(
          new ClaimedRegistration(
              outbox.getId(),
              contract.getContractNumber(),
              RegistryRegistrationRequest.from(contract, contract.getApplication()),
              outbox.getRetryCount()));
      outbox.markProcessing(claimedAt);
    }
    return claimed;
  }

  /**
   * Recovers rows stuck in {@code PROCESSING} past their lease. An expired lease is treated as a
   * failed attempt and follows the transient-retry path, so a repeatedly failing row eventually
   * lands in the dead letter instead of looping forever.
   *
   * @return the number of reclaimed rows
   */
  @Transactional
  public int reclaimStale() {
    Instant threshold = Instant.now().minus(properties.scheduler().leaseTimeout());
    List<IntegrationOutbox> stale =
        outboxRepository.findStaleProcessing(
            OutboxStatus.PROCESSING,
            threshold,
            PageRequest.of(0, properties.scheduler().batchSize()));

    for (IntegrationOutbox outbox : stale) {
      log.warn("Reclaiming outbox {} with expired processing lease", outbox.getId());
      completionService.applyTransientFailure(outbox, "lease expired");
    }
    return stale.size();
  }
}
