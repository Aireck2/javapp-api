package com.aireck.javapp.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class JwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    Collection<GrantedAuthority> authorities =
        roles == null
            ? List.of()
            : roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> "ROLE_" + role.trim().toUpperCase(Locale.ROOT))
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }
}
