package org.yandex.mymarketapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.yandex.mymarketapp.model.domain.User;
import org.yandex.mymarketapp.model.domain.UserRole;
import org.yandex.mymarketapp.repository.PostgresBaseIntegrationTest;

@AutoConfigureWebTestClient
@Import({BaseControllerTest.TestSecurityConfiguration.class})
public abstract class BaseControllerTest extends PostgresBaseIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;

    protected static final long ADMIN_ID = 101L;

    protected static final String ADMIN_USERNAME = "test_admin";

    protected static final long USER_ID = 102L;

    protected static final String USER_USERNAME = "test_user";

    @TestConfiguration
    public static class TestSecurityConfiguration {

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        UserDetailsService inMemoryUserDetailsService(PasswordEncoder passwordEncoder) {
            return username -> {
                User user = switch (username) {
                    case USER_USERNAME -> new User(USER_ID, USER_USERNAME, passwordEncoder.encode(USER_USERNAME), UserRole.USER);
                    case ADMIN_USERNAME -> new User(ADMIN_ID, ADMIN_USERNAME, passwordEncoder.encode(ADMIN_USERNAME), UserRole.ADMIN);
                    default -> {
                        System.err.println("Requested unexpected username: " + username);
                        yield null;
                    }
                };
                System.out.println("Provided test user: " + user);
                return user;
            };
        }
    }
}
