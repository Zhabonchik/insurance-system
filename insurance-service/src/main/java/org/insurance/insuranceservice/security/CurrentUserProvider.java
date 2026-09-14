package org.insurance.insuranceservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

  public static final String EMPLOYEE_ROLE = "ROLE_EMPLOYEE";

  public CurrentUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("No authenticated user in the security context");
    }
    boolean employee =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> EMPLOYEE_ROLE.equals(authority.getAuthority()));
    return new CurrentUser(authentication.getName(), employee);
  }
}
