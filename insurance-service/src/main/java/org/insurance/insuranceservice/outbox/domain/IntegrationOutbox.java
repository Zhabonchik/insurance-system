package org.insurance.insuranceservice.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.insurance.insuranceservice.contract.domain.Contract;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@Table(name = "integration_outbox")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class IntegrationOutbox {

  private static final int LAST_ERROR_MAX_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contract_id", nullable = false, unique = true)
  private Contract contract;

  @Enumerated(STRING)
  @Column(name = "status", nullable = false, length = 50)
  private OutboxStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "next_retry_at", nullable = false)
  private Instant nextRetryAt;

  @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
  private String lastError;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "claimed_at")
  private Instant claimedAt;

  public void markProcessing(Instant at) {
    this.status = OutboxStatus.PROCESSING;
    this.claimedAt = at;
    this.lastAttemptAt = at;
  }

  public void markSuccess(Instant at) {
    this.status = OutboxStatus.SUCCESS;
    this.lastAttemptAt = at;
    this.lastError = null;
  }

  public void markFailedBusiness(Instant at, String error) {
    this.status = OutboxStatus.FAILED_BUSINESS;
    this.lastAttemptAt = at;
    this.lastError = truncate(error);
  }

  public void markDeadLetter(Instant at, String error) {
    this.status = OutboxStatus.DEAD_LETTER;
    this.lastAttemptAt = at;
    this.lastError = truncate(error);
  }

  public void recordTransientFailure(Instant at, String error) {
    this.retryCount = this.retryCount + 1;
    this.lastAttemptAt = at;
    this.lastError = truncate(error);
  }

  public void scheduleRetry(Instant nextRetryAt) {
    this.status = OutboxStatus.PENDING;
    this.nextRetryAt = nextRetryAt;
  }

  private static String truncate(String error) {
    if (error == null) {
      return null;
    }
    return error.length() <= LAST_ERROR_MAX_LENGTH
        ? error
        : error.substring(0, LAST_ERROR_MAX_LENGTH);
  }
}
