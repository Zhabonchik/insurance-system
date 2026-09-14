package org.insurance.insuranceservice.outbox.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.insurance.insuranceservice.outbox.domain.IntegrationOutbox;
import org.insurance.insuranceservice.outbox.domain.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface IntegrationOutboxRepository extends JpaRepository<IntegrationOutbox, UUID> {

  Optional<IntegrationOutbox> findByContract_Id(UUID contractId);

  /**
   * Claims due PENDING rows with {@code SELECT ... FOR UPDATE SKIP LOCKED}.
   *
   * <p>The lock-timeout hint {@code -2} is Hibernate's {@code SKIP_LOCKED}: other instances skip
   * rows already locked by a concurrent worker.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  @Query(
      """
            select o from IntegrationOutbox o
            where o.status = :status and o.nextRetryAt <= :now
            order by o.nextRetryAt asc
            """)
  List<IntegrationOutbox> findDueForProcessing(
      @Param("status") OutboxStatus status, @Param("now") Instant now, Pageable pageable);

  /**
   * Claims stale {@code PROCESSING} rows whose lease has expired, using {@code SELECT ... FOR
   * UPDATE SKIP LOCKED} so concurrent reapers and active workers don't collide.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  @Query(
      """
            select o from IntegrationOutbox o
            where o.status = :status and o.claimedAt <= :threshold
            order by o.claimedAt asc
            """)
  List<IntegrationOutbox> findStaleProcessing(
      @Param("status") OutboxStatus status,
      @Param("threshold") Instant threshold,
      Pageable pageable);
}
