package org.insurance.insuranceservice.application.repository;

import jakarta.persistence.LockModeType;
import org.insurance.insuranceservice.application.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository
    extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

  /**
   * {@code SELECT ... FOR UPDATE}. Serializes concurrent approve/reject/issue on the same
   * application row.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Application a where a.id = :id")
  Optional<Application> findByIdForUpdate(@Param("id") UUID id);
}
