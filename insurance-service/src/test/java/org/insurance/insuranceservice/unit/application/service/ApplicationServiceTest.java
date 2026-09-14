package org.insurance.insuranceservice.unit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.insurance.insuranceservice.application.dto.CreateApplicationRequest;
import org.insurance.insuranceservice.application.repository.ApplicationRepository;
import org.insurance.insuranceservice.application.service.ApplicationService;
import org.insurance.insuranceservice.error.InvalidApplicationStateException;
import org.insurance.insuranceservice.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

  @Mock private ApplicationRepository applicationRepository;

  @InjectMocks private ApplicationService applicationService;

  @Test
  void createPersistsSubmittedApplicationOwnedByAuthenticatedUser() {
    when(applicationRepository.save(any(Application.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = applicationService.create(sampleRequest(), "client1");

    assertThat(response.applicantId()).isEqualTo("client1");
    assertThat(response.status()).isEqualTo(ApplicationStatus.SUBMITTED);
  }

  @Test
  void approveTransitionsSubmittedApplication() {
    Application application = application(ApplicationStatus.SUBMITTED);
    when(applicationRepository.findByIdForUpdate(application.getId()))
        .thenReturn(Optional.of(application));

    var response = applicationService.approve(application.getId());

    assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
  }

  @Test
  void approveRejectedApplicationThrows() {
    Application application = application(ApplicationStatus.REJECTED);
    when(applicationRepository.findByIdForUpdate(application.getId()))
        .thenReturn(Optional.of(application));

    assertThatThrownBy(() -> applicationService.approve(application.getId()))
        .isInstanceOf(InvalidApplicationStateException.class);
  }

  @Test
  void userCannotReadAnotherUsersApplication() {
    Application application = application(ApplicationStatus.SUBMITTED);
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

    assertThatThrownBy(
            () ->
                applicationService.get(application.getId(), new CurrentUser("someone-else", false)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void employeeCanReadAnotherUsersApplication() {
    Application application = application(ApplicationStatus.SUBMITTED);
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

    var response = applicationService.get(application.getId(), new CurrentUser("employee1", true));

    assertThat(response.id()).isEqualTo(application.getId());
  }

  private CreateApplicationRequest sampleRequest() {
    return new CreateApplicationRequest(
        "Ivan Ivanov", "1234 567890", new BigDecimal("500000.00"), 60);
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
