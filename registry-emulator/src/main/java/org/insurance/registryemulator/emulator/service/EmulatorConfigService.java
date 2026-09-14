package org.insurance.registryemulator.emulator.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.registryemulator.emulator.domain.EmulatorMode;
import org.insurance.registryemulator.emulator.domain.EmulatorConfig;
import org.insurance.registryemulator.emulator.repository.EmulatorConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmulatorConfigService {

  private final EmulatorConfigRepository repository;

  @Transactional(readOnly = true)
  public EmulatorMode currentMode() {
    return loadConfig().getMode();
  }

  @Transactional
  public EmulatorMode switchMode(@NotNull final EmulatorMode mode) {
    var currentConfig = loadConfig();
    currentConfig.updateMode(mode);
    log.info("Emulator mode switched to {}", mode);
    return currentConfig.getMode();
  }

  private EmulatorConfig loadConfig() {
    return repository
        .findById(EmulatorConfig.CONFIG_ID)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "emulator_config row id=%d is missing; Liquibase seed did not run"
                        .formatted(EmulatorConfig.CONFIG_ID)));
  }
}
