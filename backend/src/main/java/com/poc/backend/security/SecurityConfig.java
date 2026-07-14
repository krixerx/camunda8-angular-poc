package com.poc.backend.security;

import com.poc.backend.api.dto.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Single stateless JWT chain for {@code /api/**}. Role rules live here (not in annotations) so the
 * whole policy is visible in one place: applicants start processes, civil servants complete tasks,
 * any authenticated user reads.
 *
 * <p>The decoder fetches signing keys from a network-reachable JWKS URL (service DNS in Docker,
 * localhost in dev) but validates the {@code iss} claim against the one canonical public issuer
 * baked into every token by {@code KC_HOSTNAME_URL} — the standard fix for the browser-vs-container
 * hostname mismatch.
 */
@Configuration
public class SecurityConfig {

  static final String ROLE_APPLICANT = "applicant";
  static final String ROLE_CIVIL_SERVANT = "civil-servant";

  @Bean
  public SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper)
      throws Exception {
    return http.securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/api/process-definitions/*/start")
                    .hasRole(ROLE_APPLICANT)
                    .requestMatchers(HttpMethod.POST, "/api/tasks/*/complete")
                    .hasRole(ROLE_CIVIL_SERVANT)
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(
                        (request, response, e) ->
                            writeError(objectMapper, response, 401, "Authentication required"))
                    .accessDeniedHandler(
                        (request, response, e) ->
                            writeError(
                                objectMapper, response, 403, "Insufficient role for this action")))
        .build();
  }

  @Bean
  public JwtDecoder jwtDecoder(
      @Value("${keycloak.jwks-uri}") String jwksUri,
      @Value("${keycloak.issuer}") String issuer) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }

  /** Keycloak realm roles ({@code realm_access.roles[]}) → {@code ROLE_<name>} authorities. */
  private static JwtAuthenticationConverter jwtAuthenticationConverter() {
    Converter<Jwt, Collection<GrantedAuthority>> realmRoles =
        jwt -> {
          Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
          if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
          }
          return roles.stream()
              .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
              .toList();
        };
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(realmRoles);
    return converter;
  }

  /**
   * Dev CORS for the ng-serve origin calling :8085 directly. In Docker the frontend nginx proxies
   * same-origin, so this never triggers there.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }

  private static void writeError(
      ObjectMapper objectMapper, HttpServletResponse response, int status, String message)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(new ApiError(status, message)));
  }
}
