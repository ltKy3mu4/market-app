package org.yandex.mymarketapp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.SessionLimit;
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import reactor.core.publisher.Mono;

import java.nio.file.AccessDeniedException;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveAuthenticationManager authenticationManager) {
        return http
                .authenticationManager(authenticationManager)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/balance/**", "/orders/**", "/cart/**").hasRole("USER")
                        .pathMatchers("/items/**", "/").hasAnyRole("ANONYMOUS", "USER")
                )
                .anonymous(anonymous -> anonymous
                        .principal("anonymousUser")
                        .authorities("ROLE_ANONYMOUS")
                        .key("anonymousKey"))
                .formLogin(Customizer.withDefaults())
                .logout( logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler())
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
//                .sessionManagement(sessionManagement -> sessionManagement
//                        .concurrentSessions(concurrentSessions -> concurrentSessions
//                                .maximumSessions(SessionLimit.of(1))
//                                .sessionRegistry(reactiveSessionRegistry())))
//                .exceptionHandling(exceptionHandling -> exceptionHandling
//                        .accessDeniedHandler(globalExceptionHandler::handle))
                .build();
    }


    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(ReactiveUserDetailsService reactiveUserDetailsService) {
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService);
        manager.setPasswordEncoder(new BCryptPasswordEncoder());
        return manager;
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(ReactiveClientRegistrationRepository clientRegistrationRepository, ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build()
        );

        return manager;
    }
}
