package org.insurance.insuranceservice.unit.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.insurance.insuranceservice.application.repository.ApplicationRepository;
import org.insurance.insuranceservice.contract.domain.Contract;
import org.insurance.insuranceservice.contract.domain.ContractStatus;
import org.insurance.insuranceservice.contract.repository.ContractRepository;
import org.insurance.insuranceservice.contract.service.ContractIssueService;
import org.insurance.insuranceservice.contract.service.ContractNumberGenerator;
import org.insurance.insuranceservice.error.InvalidApplicationStateException;
import org.insurance.insuranceservice.outbox.domain.IntegrationOutbox;
import org.insurance.insuranceservice.outbox.domain.OutboxStatus;
import org.insurance.insuranceservice.outbox.repository.IntegrationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractIssueServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private IntegrationOutboxRepository outboxRepository;
  @Mock private ContractNumberGenerator contractNumberGenerator;

  @InjectMocks private ContractIssueService service;

  @Test
  void createsContractAndPendingOutboxForApprovedApplication() {
    Application application = application(ApplicationStatus.APPROVED);
    when(applicationRepository.findByIdForUpdate(application.getId()))
        .thenReturn(Optional.of(application));
    when(contractRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());
    when(contractNumberGenerator.nextContractNumber()).thenReturn("LIFE-2026-000001");
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.issue(application.getId());

    assertThat(result.created()).isTrue();
    assertThat(result.response().contractNumber()).isEqualTo("LIFE-2026-000001");
    assertThat(result.response().status()).isEqualTo(ContractStatus.CREATED);

    ArgumentCaptor<IntegrationOutbox> captor = ArgumentCaptor.forClass(IntegrationOutbox.class);
    verify(outboxRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(captor.getValue().getRetryCount()).isZero();
  }

  @Test
  void returnsExistingContractWithoutIssuingANewOne() {
    Application application = application(ApplicationStatus.APPROVED);
    Contract existing =
        Contract.builder()
            .id(UUID.randomUUID())
            .application(application)
            .contractNumber("LIFE-2026-000001")
            .status(ContractStatus.CREATED)
            .build();
    when(applicationRepository.findByIdForUpdate(application.getId()))
        .thenReturn(Optional.of(application));
    when(contractRepository.findByApplication_Id(application.getId()))
        .thenReturn(Optional.of(existing));

    var result = service.issue(application.getId());

    assertThat(result.created()).isFalse();
    assertThat(result.response().id()).isEqualTo(existing.getId());
    verify(contractRepository, never()).save(any());
    verify(outboxRepository, never()).save(any());
  }

  @Test
  void rejectsIssuanceForNonApprovedApplication() {
    Application application = application(ApplicationStatus.SUBMITTED);
    when(applicationRepository.findByIdForUpdate(application.getId()))
        .thenReturn(Optional.of(application));
    when(contractRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.issue(application.getId()))
        .isInstanceOf(InvalidApplicationStateException.class);

    verify(contractRepository, never()).save(any());
  }

  private Application application(ApplicationStatus status) {
    return Application.builder()
        .id(UUID.randomUUID())
        .applicantId("client1")
        .applicantName("Ivan Ivanov")
        .passportData("1234 567890")
        .insuredAmount(new BigDecimal("500000.00"))
        .termMonths(60)
        .status(status)
        .build();
  }
}
