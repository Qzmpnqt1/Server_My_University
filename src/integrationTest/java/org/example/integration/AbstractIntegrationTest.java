package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Один JVM (maxParallelForks=1), один MySQL и один Cassandra на все IT-классы — без гонок по портам и БД.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationtest")
@Import(IntegrationTestAccountNormalizer.class)
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("my_university")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    protected static final CassandraContainer<?> CASSANDRA = new CassandraContainer<>("cassandra:4.1")
            .withInitScript("integration-cassandra.cql");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.cassandra.contact-points", CASSANDRA::getHost);
        registry.add("spring.cassandra.port", () -> String.valueOf(CASSANDRA.getMappedPort(9042)));
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
        registry.add("spring.cassandra.keyspace-name", () -> "my_university_chat");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;
}
