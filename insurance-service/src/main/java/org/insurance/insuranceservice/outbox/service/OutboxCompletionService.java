package org.insurance.insuranceservice.outbox.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.insuranceservice.outbox.domain.IntegrationOutbox;
import org.insurance.insuranceservice.outbox.domain.OutboxStatus;
import org.insurance.insuranceservice.outbox.repository.IntegrationOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the outcome of a registry call in a short transaction, after the HTTP call has already
 * completed. Complementary to {@link OutboxClaimService}, which only marks rows as {@code
 * PROCESSING}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxCompletionService {

  private final IntegrationOutboxRepository outboxRepository;
  private final RetryPolicy retryPolicy;

  @Transactional
  public void succeed(UUID outboxId) {
    IntegrationOutbox outbox = processingOrNull(outboxId);
    if (outbox == null) {
      return;
    }
    outbox.markSuccess(Instant.now());
    outbox.getContract().markRegistered();
    log.info(
        "Registered contract {} in the state registry", outbox.getContract().getContractNumber());
  }

  @Transactional
  public void failPermanently(UUID outboxId, String reason) {
    IntegrationOutbox outbox = processingOrNull(outboxId);
    if (outbox == null) {
      return;
    }
    outbox.markFailedBusiness(Instant.now(), reason);
    log.warn(
        "Registry permanently rejected contract {}: {}",
        outbox.getContract().getContractNumber(),
        reason);
  }

  @Transactional
  public void failTransiently(UUID outboxId, String reason) {
    IntegrationOutbox outbox = processingOrNull(outboxId);
    if (outbox == null) {
      return;
    }
    applyTransientFailure(outbox, reason);
  }

  /**
   * Applies the exponential-backoff transition to an already-loaded entity. Callers must supply the
   * ambient transaction; used both for registry failures and for expired claims.
   */
  public void applyTransientFailure(IntegrationOutbox outbox, String reason) {
    Instant attemptAt = Instant.now();
    outbox.recordTransientFailure(attemptAt, reason);
    if (retryPolicy.isExhausted(outbox.getRetryCount())) {
      outbox.markDeadLetter(attemptAt, reason);
      log.error("Outbox record {} exhausted retries; moved to dead letter", outbox.getId());
    } else {
      Instant nextRetryAt = attemptAt.plus(retryPolicy.delayFor(outbox.getRetryCount()));
      outbox.scheduleRetry(nextRetryAt);
      log.warn(
          "Transient registry failure for outbox {}; retry #{} scheduled at {}",
          outbox.getId(),
          outbox.getRetryCount(),
          nextRetryAt);
    }
  }

  private IntegrationOutbox processingOrNull(UUID outboxId) {
    IntegrationOutbox outbox = outboxRepository.findById(outboxId).orElse(null);
    if (outbox == null || outbox.getStatus() != OutboxStatus.PROCESSING) {
      return null;
    }
    return outbox;
  }
}
