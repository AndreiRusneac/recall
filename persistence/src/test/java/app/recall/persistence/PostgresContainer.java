package app.recall.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A throwaway PostgreSQL in Docker, the same major version as docker-compose.yml.
 * {@code @ServiceConnection} points the DataSource at it. Spring caches the test context,
 * so all test classes that share the same setup also share one container.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresContainer {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16");
    }
}
