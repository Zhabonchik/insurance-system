package org.insurance.insuranceservice.unit.registry.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.insurance.insuranceservice.registry.client.RegistryErrorClassifier;
import org.junit.jupiter.api.Test;

class RegistryErrorClassifierTest {

  @Test
  void treatsServerErrorsTimeoutsAndRateLimitsAsRetryable() {
    assertThat(RegistryErrorClassifier.isRetryableStatus(500)).isTrue();
    assertThat(RegistryErrorClassifier.isRetryableStatus(503)).isTrue();
    assertThat(RegistryErrorClassifier.isRetryableStatus(408)).isTrue();
    assertThat(RegistryErrorClassifier.isRetryableStatus(429)).isTrue();
  }

  @Test
  void treatsValidationAndBusinessErrorsAsPermanent() {
    assertThat(RegistryErrorClassifier.isRetryableStatus(400)).isFalse();
    assertThat(RegistryErrorClassifier.isRetryableStatus(404)).isFalse();
    assertThat(RegistryErrorClassifier.isRetryableStatus(409)).isFalse();
    assertThat(RegistryErrorClassifier.isRetryableStatus(422)).isFalse();
  }
}
