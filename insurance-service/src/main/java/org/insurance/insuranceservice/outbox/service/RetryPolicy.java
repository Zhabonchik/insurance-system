package org.insurance.insuranceservice.outbox.service;

import java.time.Duration;

/**
 * Exponential backoff: {@code next_retry_at = now + base_delay * 2^retry_count}, capped at {@code
 * max_delay}. Retries stop once {@code retry_count >= maxRetries}.
 */
public class RetryPolicy {

  private static final int MAX_EXPONENT = 16;

  private final int maxRetries;
  private final Duration baseDelay;
  private final Duration maxDelay;

  public RetryPolicy(int maxRetries, Duration baseDelay, Duration maxDelay) {
    this.maxRetries = maxRetries;
    this.baseDelay = baseDelay;
    this.maxDelay = maxDelay;
  }

  public boolean isExhausted(int retryCount) {
    return retryCount >= maxRetries;
  }

  public Duration delayFor(int retryCount) {
    int exponent = Math.clamp(retryCount, 0, MAX_EXPONENT);
    Duration delay = baseDelay.multipliedBy(1L << exponent);
    return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
  }
}
