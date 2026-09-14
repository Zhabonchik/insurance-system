package org.insurance.insuranceservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the outbox scheduler unless explicitly disabled ({@code outbox.scheduler.enabled=false},
 * which the tests use).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "outbox.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {}
