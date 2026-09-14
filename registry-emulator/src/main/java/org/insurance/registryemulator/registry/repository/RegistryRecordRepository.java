package org.insurance.registryemulator.registry.repository;

import java.util.Optional;
import java.util.UUID;

import org.insurance.registryemulator.registry.domain.RegistryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/** Derived queries only — no JDBC / native queries (task constraint). */
public interface RegistryRecordRepository extends JpaRepository<RegistryRecord, UUID> {

  Optional<RegistryRecord> findByExternalContractId(UUID externalContractId);
}
