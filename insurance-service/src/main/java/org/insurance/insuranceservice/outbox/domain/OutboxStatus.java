package org.insurance.insuranceservice.outbox.domain;

public enum OutboxStatus {
  PENDING,
  PROCESSING,
  SUCCESS,
  FAILED_BUSINESS,
  DEAD_LETTER
}
