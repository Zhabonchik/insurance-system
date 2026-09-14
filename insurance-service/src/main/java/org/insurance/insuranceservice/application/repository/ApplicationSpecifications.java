package org.insurance.insuranceservice.application.repository;

import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import org.insurance.insuranceservice.application.domain.Application;
import org.insurance.insuranceservice.application.domain.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public final class ApplicationSpecifications {

  private static final String APPLICANT_ID = "applicantId";
  private static final String STATUS = "status";

  public static Specification<Application> filterBy(String applicantId, ApplicationStatus status) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (applicantId != null) {
        predicates.add(criteriaBuilder.equal(root.get(APPLICANT_ID), applicantId));
      }
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get(STATUS), status));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
