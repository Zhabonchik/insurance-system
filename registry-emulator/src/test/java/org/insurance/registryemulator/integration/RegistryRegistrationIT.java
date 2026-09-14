package org.insurance.registryemulator.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.insurance.registryemulator.emulator.repository.EmulatorConfigRepository;
import org.insurance.registryemulator.emulator.domain.EmulatorMode;
import org.insurance.registryemulator.emulator.service.EmulatorModeService;
import org.insurance.registryemulator.error.RegistryUnavailableException;
import org.insurance.registryemulator.registry.dto.RegistrationResult;
import org.insurance.registryemulator.registry.dto.RegistryContractRequest;
import org.insurance.registryemulator.registry.repository.RegistryRecordRepository;
import org.insurance.registryemulator.registry.service.RegistryContractService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RegistryRegistrationIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private RegistryContractService service;

  @Autowired private RegistryRecordRepository repository;

  @Autowired private EmulatorConfigRepository configRepository;

  @Autowired private EmulatorModeService modeService;

  @BeforeEach
  void reset() {
    repository.deleteAll();
    modeService.switchMode(EmulatorMode.SUCCESS);
  }

  @Test
  void liquibaseSeedsEmulatorConfig() {
    assertThat(configRepository.findById(1))
        .isPresent()
        .hasValueSatisfying(config -> assertThat(config.getMode()).isEqualTo(EmulatorMode.SUCCESS));
  }

  @Test
  void repeatedRegistrationCreatesSingleRecord() {
    RegistryContractRequest request = sample();

    RegistrationResult first = service.register(request);
    RegistrationResult second = service.register(request);

    assertThat(first.created()).isTrue();
    assertThat(second.created()).isFalse();
    assertThat(second.response().id()).isEqualTo(first.response().id());
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void unavailableModeDoesNotPersist() {
    modeService.switchMode(EmulatorMode.UNAVAILABLE);

    assertThatThrownBy(() -> service.register(sample()))
        .isInstanceOf(RegistryUnavailableException.class);
    assertThat(repository.count()).isZero();
  }

  private RegistryContractRequest sample() {
    return new RegistryContractRequest(
        UUID.randomUUID(),
        "LIFE-2026-000123",
        "Ivan Ivanov",
        "1234 567890",
        new BigDecimal("500000.00"),
        60);
  }
}
