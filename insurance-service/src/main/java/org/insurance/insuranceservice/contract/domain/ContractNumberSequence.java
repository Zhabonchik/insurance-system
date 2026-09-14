package org.insurance.insuranceservice.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/** Single-row counter ({@code id = 1}) used to build sequential contract numbers. */
@Entity
@Getter
@Table(name = "contract_number_sequence")
@NoArgsConstructor(access = PROTECTED)
public class ContractNumberSequence {

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "next_value")
  private long nextValue;

  public long next() {
    return ++nextValue;
  }
}
