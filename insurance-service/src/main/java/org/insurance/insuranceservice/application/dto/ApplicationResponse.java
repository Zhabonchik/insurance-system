package org.insurance.insuranceservice.application.dto;

import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    String applicantId,
    String applicantName,
    String passportData,
    BigDecimal insuredAmount,
    Integer termMonths,
    ApplicationStatus status,
    Instant createdAt) {

  public static ApplicationResponse from(Application application) {
    return new ApplicationResponse(
        application.getId(),
        application.getApplicantId(),
        application.getApplicantName(),
        application.getPassportData(),
        application.getInsuredAmount(),
        application.getTermMonths(),
        application.getStatus(),
        application.getCreatedAt());
  }
}
