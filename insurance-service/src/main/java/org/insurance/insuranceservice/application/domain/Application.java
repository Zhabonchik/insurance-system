package org.insurance.insuranceservice.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.insurance.insuranceservice.error.InvalidApplicationStateException;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@Table(name = "application")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Application {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @Column(name = "applicant_id")
  private String applicantId;

  @Column(name = "applicant_name")
  private String applicantName;

  @Column(name = "passport_data")
  private String passportData;

  @Column(name = "insured_amount")
  private BigDecimal insuredAmount;

  @Column(name = "term_months")
  private Integer termMonths;

  @Enumerated(STRING)
  @Column(name = "status")
  private ApplicationStatus status;

  @CreatedDate
  @Column(name = "created_at")
  private Instant createdAt;

  public void approve() {
    transitionTo(ApplicationStatus.APPROVED);
  }

  public void reject() {
    transitionTo(ApplicationStatus.REJECTED);
  }

  public boolean isApproved() {
    return status == ApplicationStatus.APPROVED;
  }

  private void transitionTo(ApplicationStatus target) {
    if (status != ApplicationStatus.SUBMITTED) {
      throw new InvalidApplicationStateException(
          "Application %s is in %s state and cannot be moved to %s".formatted(id, status, target));
    }
    this.status = target;
  }
}
