package org.insurance.registryemulator.registry.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record RegistryContractRequest(
    @NotNull UUID externalContractId,
    @NotBlank @Size(max = 100) String contractNumber,
    @NotBlank @Size(max = 255) String policyholderName,
    @NotBlank @Size(max = 50) String policyholderPassport,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal insuredAmount,
    @NotNull @Positive Integer termMonths) {}
