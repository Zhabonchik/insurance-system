package org.insurance.registryemulator.emulator.repository;

import org.insurance.registryemulator.emulator.domain.EmulatorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmulatorConfigRepository extends JpaRepository<EmulatorConfig, Integer> {}
