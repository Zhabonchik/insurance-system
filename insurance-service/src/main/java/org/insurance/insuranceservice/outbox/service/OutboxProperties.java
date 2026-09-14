package org.insurance.insuranceservice.outbox.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(Scheduler scheduler, Retry retry) {

  public record Scheduler(
      boolean enabled, Duration fixedDelay, int batchSize, int concurrency, Duration leaseTimeout) {}

  public record Retry(int maxRetries, Duration baseDelay, Duration maxDelay) {}
}
