package org.insurance.insuranceservice.unit.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.insurance.insuranceservice.outbox.service.ClaimedRegistration;
import org.insurance.insuranceservice.outbox.service.OutboxClaimService;
import org.insurance.insuranceservice.outbox.service.OutboxProperties;
import org.insurance.insuranceservice.outbox.service.OutboxScheduler;
import org.insurance.insuranceservice.outbox.service.OutboxWorker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

  @Mock private OutboxWorker worker;
  @Mock private OutboxClaimService claimService;

  private ExecutorService executor;

  @AfterEach
  void shutdownExecutor() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  @Test
  void boundsConcurrentDispatchesByConcurrency() throws Exception {
    executor = Executors.newVirtualThreadPerTaskExecutor();
    int concurrency = 5;
    var claimed = new ClaimedRegistration(UUID.randomUUID(), "C-1", null, 0);
    when(claimService.claim(anyInt())).thenReturn(List.of(claimed));

    CountDownLatch started = new CountDownLatch(concurrency);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger current = new AtomicInteger();
    AtomicInteger max = new AtomicInteger();
    doAnswer(
            invocation -> {
              max.accumulateAndGet(current.incrementAndGet(), Math::max);
              started.countDown();
              release.await();
              current.decrementAndGet();
              return null;
            })
        .when(worker)
        .process(any());

    var scheduler = new OutboxScheduler(worker, claimService, properties(concurrency), executor);

    ExecutorService poller = Executors.newSingleThreadExecutor();
    try {
      Future<?> poll = poller.submit(scheduler::poll);
      assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(max.get()).isEqualTo(concurrency);
      release.countDown();
      poll.get(5, TimeUnit.SECONDS);
    } finally {
      poller.shutdownNow();
    }
    assertThat(max.get()).isEqualTo(concurrency);
  }

  @Test
  void failedTaskDoesNotFailThePoll() {
    executor = Executors.newVirtualThreadPerTaskExecutor();
    var claimed = new ClaimedRegistration(UUID.randomUUID(), "C-1", null, 0);
    when(claimService.claim(anyInt())).thenReturn(List.of(claimed)).thenReturn(List.of());
    doThrow(new IllegalStateException("boom")).when(worker).process(any());

    var scheduler = new OutboxScheduler(worker, claimService, properties(2), executor);

    assertThatCode(scheduler::poll).doesNotThrowAnyException();
  }

  private OutboxProperties properties(int concurrency) {
    return new OutboxProperties(
        new OutboxProperties.Scheduler(
            true, Duration.ofSeconds(5), 100, concurrency, Duration.ofSeconds(30)),
        new OutboxProperties.Retry(5, Duration.ofSeconds(2), Duration.ofMinutes(5)));
  }
}
