package org.yandex.paymentservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(exchanges -> exchanges
            .anyExchange().authenticated())
        .csrf(CsrfSpec::disable)
        .oauth2ResourceServer(server -> server
            .jwt(jwtSpec -> {
              ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
              jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                List<String> scopes = jwt.getClaim("scope");
                return Flux.fromIterable(scopes)
                    .map(scope -> "SCOPE_" + scope.toUpperCase())
                    .map(SimpleGrantedAuthority::new);
              });
              jwtSpec.jwtAuthenticationConverter(jwtAuthenticationConverter);
            })
        ).build();
  }

}
