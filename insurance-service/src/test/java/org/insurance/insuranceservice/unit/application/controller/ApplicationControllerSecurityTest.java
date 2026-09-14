package org.insurance.insuranceservice.unit.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.insurance.insuranceservice.application.controller.ApplicationController;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.insurance.insuranceservice.application.dto.ApplicationResponse;
import org.insurance.insuranceservice.application.service.ApplicationService;
import org.insurance.insuranceservice.contract.service.ContractIssueService;
import org.insurance.insuranceservice.security.CurrentUser;
import org.insurance.insuranceservice.security.CurrentUserProvider;
import org.insurance.insuranceservice.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerSecurityTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ApplicationService applicationService;
  @MockitoBean private ContractIssueService contractIssueService;
  @MockitoBean private CurrentUserProvider currentUserProvider;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void rejectsAnonymousRequests() throws Exception {
    mockMvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsUserWithoutEmployeeRoleWhenApproving() throws Exception {
    mockMvc
        .perform(
            post("/api/applications/{id}/approve", UUID.randomUUID())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void allowsUserToCreateApplication() throws Exception {
    given(currentUserProvider.currentUser()).willReturn(new CurrentUser("client1", false));
    given(applicationService.create(any(), any()))
        .willReturn(
            new ApplicationResponse(
                UUID.randomUUID(),
                "client1",
                "Ivan Ivanov",
                "1234 567890",
                new BigDecimal("500000.00"),
                60,
                ApplicationStatus.SUBMITTED,
                Instant.now()));

    mockMvc
        .perform(
            post("/api/applications")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"applicantName":"Ivan Ivanov","passportData":"1234 567890",
                                         "insuredAmount":500000.00,"termMonths":60}
                                        """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.applicantId").value("client1"));
  }
}
