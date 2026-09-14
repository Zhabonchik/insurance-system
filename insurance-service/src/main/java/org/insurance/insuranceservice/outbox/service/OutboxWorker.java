package org.insurance.insuranceservice.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.insuranceservice.registry.client.RegistryClient;
import org.insurance.insuranceservice.registry.error.RegistryPermanentException;
import org.insurance.insuranceservice.registry.error.RegistryTransientException;
import org.springframework.stereotype.Component;

/**
 * Orchestrates a single outbox item: the registry call runs outside any transaction, while the
 * claim and the result write happen in short transactions owned by {@link OutboxClaimService} and
 * {@link OutboxCompletionService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

  private final OutboxClaimService claimService;
  private final OutboxCompletionService completionService;
  private final RegistryClient registryClient;

  /**
   * Claims and processes a single due record synchronously.
   *
   * @return {@code true} if a record was claimed and processed, {@code false} if none was due.
   */
  public boolean processNext() {
    var claimed = claimService.claim(1);
    if (claimed.isEmpty()) {
      return false;
    }
    process(claimed.getFirst());
    return true;
  }

  /** Processes an already-claimed record without opening a transaction around the registry call. */
  public void process(ClaimedRegistration claimed) {
    try {
      registryClient.register(claimed.request());
      completionService.succeed(claimed.outboxId());
    } catch (RegistryPermanentException ex) {
      completionService.failPermanently(claimed.outboxId(), ex.getMessage());
    } catch (RegistryTransientException ex) {
      completionService.failTransiently(claimed.outboxId(), ex.getMessage());
    }
  }
}
