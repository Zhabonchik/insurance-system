package org.insurance.insuranceservice.integration.contract.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.insurance.insuranceservice.contract.service.ContractIssueService;
import org.insurance.insuranceservice.integration.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContractIssueServiceIT extends BaseIT {

  private static final int ATTEMPTS = 16;

  @Autowired private ContractIssueService contractIssueService;

  @Test
  void concurrentIssuanceCreatesExactlyOneContractAndOutboxRecord() throws Exception {
    UUID applicationId = persistApprovedApplication().getId();

    ExecutorService executor = Executors.newFixedThreadPool(ATTEMPTS);
    CountDownLatch ready = new CountDownLatch(ATTEMPTS);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<UUID>> futures = new ArrayList<>();

    try {
      for (int i = 0; i < ATTEMPTS; i++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return contractIssueService.issue(applicationId).response().id();
                }));
      }

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      Set<UUID> contractIds = new HashSet<>();
      for (Future<UUID> future : futures) {
        contractIds.add(future.get(30, TimeUnit.SECONDS));
      }
      assertThat(contractIds).hasSize(1);
    } finally {
      executor.shutdownNow();
    }

    assertThat(contractRepository.count()).isEqualTo(1);
    assertThat(outboxRepository.count()).isEqualTo(1);
    assertThat(contractRepository.findByApplication_Id(applicationId)).isPresent();
  }
}
