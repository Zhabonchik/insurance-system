package org.insurance.registryemulator.emulator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@Table(name = "emulator_config")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class EmulatorConfig {

  public static final int CONFIG_ID = 1;

  @Id
  @NotNull
  @Column(name = "id")
  private Integer id;

  @NotNull
  @Enumerated(STRING)
  @Column(name = "mode", length = 50)
  private EmulatorMode mode;

  public void updateMode(final EmulatorMode mode) {
    this.mode = mode;
  }
}
