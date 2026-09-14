package org.insurance.insuranceservice.application.dto;

import org.insurance.insuranceservice.application.domain.ApplicationStatus;

/** Resolved filter: {@code applicantId} is already constrained for USER callers. */
public record ApplicationQuery(String applicantId, ApplicationStatus status) {}
