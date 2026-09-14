package org.insurance.insuranceservice.contract.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.insurance.insuranceservice.contract.domain.Contract;
import org.insurance.insuranceservice.contract.domain.ContractStatus;
import org.insurance.insuranceservice.contract.dto.ContractResponse;
import org.insurance.insuranceservice.contract.repository.ContractRepository;
import org.insurance.insuranceservice.contract.repository.ContractSpecifications;
import org.insurance.insuranceservice.error.ResourceNotFoundException;
import org.insurance.insuranceservice.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractService {

  private final ContractRepository contractRepository;

  @Transactional(readOnly = true)
  public Page<ContractResponse> list(
      String applicantId, ContractStatus status, String contractNumber, Pageable pageable) {
    return contractRepository
        .findAll(ContractSpecifications.filterBy(applicantId, status, contractNumber), pageable)
        .map(ContractResponse::from);
  }

  @Transactional(readOnly = true)
  public ContractResponse get(UUID id, CurrentUser currentUser) {
    Contract contract =
        contractRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contract", id));
    if (!currentUser.employee()
        && !contract.getApplication().getApplicantId().equals(currentUser.id())) {
      throw new AccessDeniedException(
          "Contract %s does not belong to the current user".formatted(id));
    }
    return ContractResponse.from(contract);
  }
}
