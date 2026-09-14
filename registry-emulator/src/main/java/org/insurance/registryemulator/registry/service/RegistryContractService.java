package org.insurance.registryemulator.registry.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.registryemulator.emulator.domain.EmulatorMode;
import org.insurance.registryemulator.emulator.service.EmulatorModeService;
import org.insurance.registryemulator.error.RegistryBusinessException;
import org.insurance.registryemulator.error.RegistryRecordNotFoundException;
import org.insurance.registryemulator.error.RegistryTechnicalException;
import org.insurance.registryemulator.error.RegistryUnavailableException;
import org.insurance.registryemulator.registry.domain.RegistryRecord;
import org.insurance.registryemulator.registry.dto.RegistrationResult;
import org.insurance.registryemulator.registry.dto.RegistryContractRequest;
import org.insurance.registryemulator.registry.dto.RegistryContractResponse;
import org.insurance.registryemulator.registry.repository.RegistryRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistryContractService {

  private final RegistryRecordWriter writer;
  private final EmulatorModeService modeService;
  private final RegistryRecordRepository repository;

  /**
   * Register a contract idempotently.
   *
   * <p>Not wrapped in a single transaction: each repository call owns its own transaction, and the
   * insert itself is isolated in {@link RegistryRecordWriter} so a duplicate-key race does not
   * poison an outer transaction.
   */
  public RegistrationResult register(RegistryContractRequest request) {
    applyBehaviorMode();

    return repository
        .findByExternalContractId(request.externalContractId())
        .map(
            existing -> {
              log.info(
                  "Duplicate registration for externalContractId={}; returning existing record",
                  request.externalContractId());
              return new RegistrationResult(RegistryContractResponse.from(existing), false);
            })
        .orElseGet(() -> insertOrReturnExisting(request));
  }

  @Transactional(readOnly = true)
  public Page<RegistryContractResponse> list(Pageable pageable) {
    return repository.findAll(pageable).map(RegistryContractResponse::from);
  }

  @Transactional(readOnly = true)
  public RegistryContractResponse getById(UUID externalContractId) {
    return repository
        .findByExternalContractId(externalContractId)
        .map(RegistryContractResponse::from)
        .orElseThrow(() -> new RegistryRecordNotFoundException(externalContractId));
  }

  private RegistrationResult insertOrReturnExisting(RegistryContractRequest request) {
    try {
      RegistryRecord saved = writer.insert(request);
      log.info("Registered contract externalContractId={}", request.externalContractId());
      return new RegistrationResult(RegistryContractResponse.from(saved), true);
    } catch (DataIntegrityViolationException ex) {
      // Lost the race against a concurrent insert with the same external id.
      RegistryRecord winner =
          repository.findByExternalContractId(request.externalContractId()).orElseThrow(() -> ex);
      log.info(
          "Concurrent duplicate detected for externalContractId={}; returning winner",
          request.externalContractId());
      return new RegistrationResult(RegistryContractResponse.from(winner), false);
    }
  }

  private void applyBehaviorMode() {
    EmulatorMode mode = modeService.currentMode();
    switch (mode) {
      case SUCCESS -> {
        // proceed
      }
      case BUSINESS_ERROR ->
          throw new RegistryBusinessException(
              "Registry rejected the contract due to a business validation error");
      case TECHNICAL_ERROR ->
          throw new RegistryTechnicalException(
              "Registry failed with a technical error; retry later");
      case UNAVAILABLE ->
          throw new RegistryUnavailableException(
              "Registry is temporarily unavailable; retry later");
    }
  }
}
