package org.insurance.insuranceservice.application.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.insurance.insuranceservice.application.dto.ApplicationQuery;
import org.insurance.insuranceservice.application.dto.ApplicationResponse;
import org.insurance.insuranceservice.application.dto.CreateApplicationRequest;
import org.insurance.insuranceservice.application.service.ApplicationService;
import org.insurance.insuranceservice.contract.dto.ContractIssueResult;
import org.insurance.insuranceservice.contract.dto.ContractResponse;
import org.insurance.insuranceservice.contract.service.ContractIssueService;
import org.insurance.insuranceservice.security.CurrentUser;
import org.insurance.insuranceservice.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private static final String CREATED_AT = "createdAt";

    private final ApplicationService applicationService;
    private final ContractIssueService contractIssueService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody CreateApplicationRequest request) {
        return applicationService.create(request, currentUserProvider.currentUser().id());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE')")
    public Page<ApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String applicantId,
            @PageableDefault(size = 20, sort = CREATED_AT, direction = Sort.Direction.DESC)
            Pageable pageable) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        String applicantFilter = currentUser.employee() ? applicantId : currentUser.id();
        return applicationService.list(new ApplicationQuery(applicantFilter, status), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE')")
    public ApplicationResponse get(@PathVariable UUID id) {
        return applicationService.get(id, currentUserProvider.currentUser());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApplicationResponse approve(@PathVariable UUID id) {
        return applicationService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApplicationResponse reject(@PathVariable UUID id) {
        return applicationService.reject(id);
    }

    /** Idempotent: 201 on first issue, 200 when the contract already exists. */
    @PostMapping("/{id}/contract")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ContractResponse> issue(@PathVariable UUID id) {
        ContractIssueResult result = contractIssueService.issue(id);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }
}
