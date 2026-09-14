package org.insurance.insuranceservice.contract.repository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import org.insurance.insuranceservice.contract.domain.Contract;
import org.insurance.insuranceservice.contract.domain.ContractStatus;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public final class ContractSpecifications {

  private static final String APPLICANT_ID = "applicantId";
  private static final String APPLICATION = "application";
  private static final String CONTRACT_NUMBER = "contractNumber";
  private static final String STATUS = "status";

  public static Specification<Contract> filterBy(
      String applicantId, ContractStatus status, String contractNumber) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (applicantId != null) {
        predicates.add(
            criteriaBuilder.equal(
                root.join(APPLICATION, JoinType.INNER).get(APPLICANT_ID), applicantId));
      }
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get(STATUS), status));
      }
      if (contractNumber != null) {
        predicates.add(criteriaBuilder.equal(root.get(CONTRACT_NUMBER), contractNumber));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
