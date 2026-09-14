package org.insurance.registryemulator.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@Table(name = "registry_record")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RegistryRecord {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "external_contract_id")
  private UUID externalContractId;

  @Column(name = "contract_number")
  private String contractNumber;

  @Column(name = "policyholder_name")
  private String policyholderName;

  @Column(name = "policyholder_passport")
  private String policyholderPassport;

  @Column(name = "insured_amount")
  private BigDecimal insuredAmount;

  @Column(name = "term_months")
  private Integer termMonths;

  @CreatedDate
  @Column(name = "registered_at")
  private Instant registeredAt;
}
