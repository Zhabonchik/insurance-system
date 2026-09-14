package org.insurance.insuranceservice.unit.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.insurance.insuranceservice.outbox.service.RetryPolicy;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

  private final RetryPolicy retryPolicy =
      new RetryPolicy(5, Duration.ofSeconds(2), Duration.ofMinutes(5));

  @Test
  void doublesDelayForEachAttempt() {
    assertThat(retryPolicy.delayFor(0)).isEqualTo(Duration.ofSeconds(2));
    assertThat(retryPolicy.delayFor(1)).isEqualTo(Duration.ofSeconds(4));
    assertThat(retryPolicy.delayFor(2)).isEqualTo(Duration.ofSeconds(8));
    assertThat(retryPolicy.delayFor(3)).isEqualTo(Duration.ofSeconds(16));
  }

  @Test
  void capsDelayAtMaximum() {
    assertThat(retryPolicy.delayFor(20)).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void reportsExhaustionAtMaxRetries() {
    assertThat(retryPolicy.isExhausted(4)).isFalse();
    assertThat(retryPolicy.isExhausted(5)).isTrue();
    assertThat(retryPolicy.isExhausted(6)).isTrue();
  }
}
