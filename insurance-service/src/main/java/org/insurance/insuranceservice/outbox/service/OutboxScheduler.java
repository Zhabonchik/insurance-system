package org.insurance.insuranceservice.outbox.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox and dispatches up to {@code concurrency} registrations in parallel on virtual
 * threads. A semaphore bounds in-flight work so the database connection pool is never exhausted.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "outbox.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxScheduler {

  private final OutboxWorker worker;
  private final OutboxClaimService claimService;
  private final OutboxProperties properties;
  private final ExecutorService executor;

  public OutboxScheduler(
      OutboxWorker worker,
      OutboxClaimService claimService,
      OutboxProperties properties,
      @Qualifier("outboxExecutor") ExecutorService executor) {
    this.worker = worker;
    this.claimService = claimService;
    this.properties = properties;
    this.executor = executor;
  }

  @Scheduled(fixedDelayString = "${outbox.scheduler.fixed-delay:5s}")
  public void poll() {
    claimService.reclaimStale();

    int remaining = properties.scheduler().batchSize();
    int concurrency = properties.scheduler().concurrency();
    Semaphore slots = new Semaphore(concurrency);
    List<CompletableFuture<Void>> inFlight = new ArrayList<>();

    try {
      while (remaining > 0) {
        List<ClaimedRegistration> claimed = claimService.claim(Math.min(remaining, concurrency));
        if (claimed.isEmpty()) {
          break;
        }
        remaining -= claimed.size();
        for (ClaimedRegistration item : claimed) {
          slots.acquire();
          inFlight.add(dispatch(item, slots));
        }
      }
      CompletableFuture.allOf(inFlight.toArray(CompletableFuture[]::new)).join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("Outbox poll interrupted with {} dispatch(es) in flight", inFlight.size());
    } catch (CompletionException ex) {
      log.error("Outbox poll finished with an unexpected failure", ex);
    }
  }

  private CompletableFuture<Void> dispatch(ClaimedRegistration item, Semaphore slots) {
    return CompletableFuture.runAsync(() -> worker.process(item), executor)
        .whenComplete(
            (ignored, ex) -> {
              if (ex != null) {
                log.error("Unexpected failure processing outbox {}", item.outboxId(), ex);
              }
              slots.release();
            });
  }
}
