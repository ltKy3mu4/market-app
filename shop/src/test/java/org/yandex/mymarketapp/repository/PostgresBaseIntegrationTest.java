package org.yandex.mymarketapp.repository;


import com.redis.testcontainers.RedisContainer;
import lombok.SneakyThrows;
import org.junit.platform.commons.support.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@SpringBootTest
@Testcontainers
@DirtiesContext
public abstract class PostgresBaseIntegrationTest {

    @Autowired
    protected DatabaseClient databaseClient;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s", postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName());
        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName());
        registry.add("spring.liquibase.url", () -> jdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);
    }

    @Container
    static final RedisContainer redisContainer = new RedisContainer("redis:7.0.11-alpine");

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getRedisPort);
//        registry.add("spring.data.redis.username", redisContainer::get);
    }



    @SneakyThrows
    protected void executeSqlScript(String scriptPath) {
        try {
            ClassPathResource resource = new ClassPathResource(scriptPath);
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Execute the entire script
            databaseClient.sql(sql)
                    .fetch()
                    .rowsUpdated()
                    .block(Duration.ofSeconds(5));

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL script: " + scriptPath, e);
        }
    }
}
