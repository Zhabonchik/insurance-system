package org.insurance.insuranceservice.application.service;

import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.insurance.insuranceservice.application.dto.ApplicationQuery;
import org.insurance.insuranceservice.application.dto.ApplicationResponse;
import org.insurance.insuranceservice.application.dto.CreateApplicationRequest;
import org.insurance.insuranceservice.application.repository.ApplicationRepository;
import org.insurance.insuranceservice.application.repository.ApplicationSpecifications;
import org.insurance.insuranceservice.error.ResourceNotFoundException;
import org.insurance.insuranceservice.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

  private final ApplicationRepository applicationRepository;

  @Transactional
  public ApplicationResponse create(CreateApplicationRequest request, String applicantId) {
    Application application =
        Application.builder()
            .applicantId(applicantId)
            .applicantName(request.applicantName())
            .passportData(request.passportData())
            .insuredAmount(request.insuredAmount())
            .termMonths(request.termMonths())
            .status(ApplicationStatus.SUBMITTED)
            .build();
    Application saved = applicationRepository.save(application);
    log.info("Created application {} for applicant {}", saved.getId(), applicantId);
    return ApplicationResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public ApplicationResponse get(UUID id, CurrentUser currentUser) {
    Application application =
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    assertReadable(application, currentUser);
    return ApplicationResponse.from(application);
  }

  @Transactional(readOnly = true)
  public Page<ApplicationResponse> list(ApplicationQuery query, Pageable pageable) {
    return applicationRepository
        .findAll(ApplicationSpecifications.filterBy(query.applicantId(), query.status()), pageable)
        .map(ApplicationResponse::from);
  }

  @Transactional
  public ApplicationResponse approve(UUID id) {
    return transition(id, Application::approve);
  }

  @Transactional
  public ApplicationResponse reject(UUID id) {
    return transition(id, Application::reject);
  }

  private ApplicationResponse transition(UUID id, Consumer<Application> action) {
    Application application =
        applicationRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    action.accept(application);
    log.info("Application {} transitioned to {}", id, application.getStatus());
    return ApplicationResponse.from(application);
  }

  private void assertReadable(Application application, CurrentUser currentUser) {
    if (!currentUser.employee() && !application.getApplicantId().equals(currentUser.id())) {
      throw new AccessDeniedException(
          "Application %s does not belong to the current user".formatted(application.getId()));
    }
  }
}
