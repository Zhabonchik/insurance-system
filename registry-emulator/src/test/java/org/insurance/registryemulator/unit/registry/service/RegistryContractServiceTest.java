package org.insurance.registryemulator.unit.registry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.insurance.registryemulator.emulator.domain.EmulatorMode;
import org.insurance.registryemulator.emulator.service.EmulatorModeService;
import org.insurance.registryemulator.error.RegistryBusinessException;
import org.insurance.registryemulator.error.RegistryTechnicalException;
import org.insurance.registryemulator.error.RegistryUnavailableException;
import org.insurance.registryemulator.registry.domain.RegistryRecord;
import org.insurance.registryemulator.registry.dto.RegistrationResult;
import org.insurance.registryemulator.registry.dto.RegistryContractRequest;
import org.insurance.registryemulator.registry.repository.RegistryRecordRepository;
import org.insurance.registryemulator.registry.service.RegistryContractService;
import org.insurance.registryemulator.registry.service.RegistryRecordWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RegistryContractServiceTest {

  @Mock private RegistryRecordRepository repository;

  @Mock private EmulatorModeService modeService;

  @Mock private RegistryRecordWriter writer;

  @InjectMocks private RegistryContractService service;

  private final RegistryContractRequest request =
      new RegistryContractRequest(
          UUID.randomUUID(),
          "LIFE-2026-000123",
          "Ivan Ivanov",
          "1234 567890",
          new BigDecimal("500000.00"),
          60);

  @Test
  void returnsExistingRecordWithoutInserting() {
    when(modeService.currentMode()).thenReturn(EmulatorMode.SUCCESS);
    RegistryRecord existing = record(request.externalContractId());
    when(repository.findByExternalContractId(request.externalContractId()))
        .thenReturn(Optional.of(existing));

    RegistrationResult result = service.register(request);

    assertThat(result.created()).isFalse();
    assertThat(result.response().externalContractId()).isEqualTo(request.externalContractId());
    verify(writer, never()).insert(any());
  }

  @Test
  void insertsNewRecordInSuccessMode() {
    when(modeService.currentMode()).thenReturn(EmulatorMode.SUCCESS);
    when(repository.findByExternalContractId(any())).thenReturn(Optional.empty());
    RegistryRecord saved = record(request.externalContractId());
    when(writer.insert(any())).thenReturn(saved);

    RegistrationResult result = service.register(request);

    assertThat(result.created()).isTrue();
    verify(writer).insert(request);
  }

  @Test
  void returnsWinnerWhenConcurrentInsertWinsTheRace() {
    when(modeService.currentMode()).thenReturn(EmulatorMode.SUCCESS);
    RegistryRecord winner = record(request.externalContractId());
    when(repository.findByExternalContractId(request.externalContractId()))
        .thenReturn(Optional.empty(), Optional.of(winner));
    when(writer.insert(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

    RegistrationResult result = service.register(request);

    assertThat(result.created()).isFalse();
    assertThat(result.response().id()).isEqualTo(winner.getId());
  }

  @Test
  void businessErrorModeRethrowsAsBusinessException() {
    when(modeService.currentMode()).thenReturn(EmulatorMode.BUSINESS_ERROR);
    assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(RegistryBusinessException.class);
    verify(writer, never()).insert(any());
  }

  @Test
  void technicalErrorModeRethrowsAsTechnicalException() {
    when(modeService.currentMode()).thenReturn(EmulatorMode.TECHNICAL_ERROR);
    assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(RegistryTechnicalException.class);
  }

  @Test
  void unavailableModeRethrowsAsUnavailableException() {
    when(modeService.currentMode()).thenReturn(EmulatorMode.UNAVAILABLE);
    assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(RegistryUnavailableException.class);
  }

  private RegistryRecord record(UUID externalContractId) {
    return RegistryRecord.builder()
        .id(UUID.randomUUID())
        .externalContractId(externalContractId)
        .contractNumber(request.contractNumber())
        .policyholderName(request.policyholderName())
        .policyholderPassport(request.policyholderPassport())
        .insuredAmount(request.insuredAmount())
        .termMonths(request.termMonths())
        .build();
  }
}
