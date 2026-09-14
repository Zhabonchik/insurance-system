package org.insurance.insuranceservice.contract.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.insurance.insuranceservice.contract.domain.ContractNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractNumberSequenceRepository
    extends JpaRepository<ContractNumberSequence, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ContractNumberSequence s where s.id = :id")
  Optional<ContractNumberSequence> findForUpdate(@Param("id") Integer id);
}
