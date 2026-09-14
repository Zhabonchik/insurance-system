package org.insurance.insuranceservice.integration.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.insurance.insuranceservice.application.dto.CreateApplicationRequest;
import org.insurance.insuranceservice.application.service.ApplicationService;
import org.insurance.insuranceservice.integration.BaseIT;
import org.insurance.insuranceservice.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationServiceIT extends BaseIT {

  @Autowired private ApplicationService applicationService;

  @Test
  void createThenApprovePersistsTransitions() {
    var created =
        applicationService.create(
            new CreateApplicationRequest(
                "Ivan Ivanov", "1234 567890", new BigDecimal("500000.00"), 60),
            "client1");

    assertThat(created.status()).isEqualTo(ApplicationStatus.SUBMITTED);
    assertThat(applicationService.get(created.id(), new CurrentUser("client1", false))).isNotNull();

    var approved = applicationService.approve(created.id());

    assertThat(approved.status()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(applicationRepository.findById(created.id()).orElseThrow().getStatus())
        .isEqualTo(ApplicationStatus.APPROVED);
  }
}
