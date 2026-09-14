package org.insurance.insuranceservice.outbox.service;

import java.util.UUID;
import org.insurance.insuranceservice.registry.dto.RegistryRegistrationRequest;

/**
 * Immutable snapshot of a claimed outbox row, built inside the claim transaction so the registry
 * call can run without holding a database connection or lock.
 */
public record ClaimedRegistration(
    UUID outboxId, String contractNumber, RegistryRegistrationRequest request, int retryCount) {}
