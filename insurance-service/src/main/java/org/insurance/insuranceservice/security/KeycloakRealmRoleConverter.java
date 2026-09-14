package org.insurance.insuranceservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Maps Keycloak's nested {@code realm_access.roles} claim to {@code ROLE_*} authorities.
 *
 * <p>The built-in {@code JwtGrantedAuthoritiesConverter} reads a flat claim via {@code
 * Jwt.getClaim(name)} and therefore cannot traverse {@code realm_access.roles}; a custom converter
 * is required.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_PROPERTY = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
    if (realmAccess == null) {
      return List.of();
    }
    Object roles = realmAccess.get(ROLES_PROPERTY);
    if (!(roles instanceof Collection<?> roleNames)) {
      return List.of();
    }
    return roleNames.stream()
        .map(String::valueOf)
        .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
        .map(GrantedAuthority.class::cast)
        .toList();
  }
}
