package org.insurance.insuranceservice.integration;

import java.math.BigDecimal;
import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.insurance.insuranceservice.application.repository.ApplicationRepository;
import org.insurance.insuranceservice.contract.repository.ContractRepository;
import org.insurance.insuranceservice.outbox.repository.IntegrationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @MockitoBean protected JwtDecoder jwtDecoder;

  @Autowired protected ApplicationRepository applicationRepository;

  @Autowired protected ContractRepository contractRepository;

  @Autowired protected IntegrationOutboxRepository outboxRepository;

  @BeforeEach
  void resetDatabase() {
    outboxRepository.deleteAll();
    contractRepository.deleteAll();
    applicationRepository.deleteAll();
  }

  protected Application persistApprovedApplication() {
    return applicationRepository.saveAndFlush(
        Application.builder()
            .applicantId("client1")
            .applicantName("Ivan Ivanov")
            .passportData("1234 567890")
            .insuredAmount(new BigDecimal("500000.00"))
            .termMonths(60)
            .status(ApplicationStatus.APPROVED)
            .build());
  }
}
