package org.insurance.insuranceservice.outbox.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.insurance.insuranceservice.outbox.service.OutboxProperties;
import org.insurance.insuranceservice.outbox.service.RetryPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {

  @Bean
  RetryPolicy retryPolicy(OutboxProperties properties) {
    OutboxProperties.Retry retry = properties.retry();
    return new RetryPolicy(retry.maxRetries(), retry.baseDelay(), retry.maxDelay());
  }

  /**
   * Virtual-thread executor for outbox dispatch. In-flight work is bounded by the scheduler's
   * semaphore, not by this executor, so it stays cheap even when idle.
   */
  @Bean(destroyMethod = "close")
  ExecutorService outboxExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
