package org.insurance.insuranceservice.security;

/**
 * The authenticated caller: {@code id} is the JWT {@code sub}, {@code employee}
 * indicates the {@code EMPLOYEE} realm role is present.
 */
public record CurrentUser(String id, boolean employee) {}
