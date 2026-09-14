package org.insurance.insuranceservice.application.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateApplicationRequest(
    @NotBlank @Size(max = 255) String applicantName,
    @NotBlank @Size(max = 50) String passportData,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal insuredAmount,
    @NotNull @Positive Integer termMonths) {}
