package org.insurance.insuranceservice.contract.repository;

import org.insurance.insuranceservice.contract.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository
    extends JpaRepository<Contract, UUID>, JpaSpecificationExecutor<Contract> {

  Optional<Contract> findByApplication_Id(UUID applicationId);

  boolean existsByApplication_Id(UUID applicationId);
}
