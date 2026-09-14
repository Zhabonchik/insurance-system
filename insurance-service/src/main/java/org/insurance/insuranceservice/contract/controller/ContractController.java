package org.insurance.insuranceservice.contract.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.insurance.insuranceservice.contract.domain.ContractStatus;
import org.insurance.insuranceservice.contract.dto.ContractResponse;
import org.insurance.insuranceservice.contract.service.ContractService;
import org.insurance.insuranceservice.security.CurrentUser;
import org.insurance.insuranceservice.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

  private static final String CREATED_AT = "createdAt";

  private final ContractService contractService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping
  @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE')")
  public Page<ContractResponse> list(
      @RequestParam(required = false) ContractStatus status,
      @RequestParam(required = false) String contractNumber,
      @PageableDefault(size = 20, sort = CREATED_AT, direction = Sort.Direction.DESC)
          Pageable pageable) {
    CurrentUser currentUser = currentUserProvider.currentUser();
    String applicantFilter = currentUser.employee() ? null : currentUser.id();
    return contractService.list(applicantFilter, status, contractNumber, pageable);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE')")
  public ContractResponse get(@PathVariable UUID id) {
    return contractService.get(id, currentUserProvider.currentUser());
  }
}
