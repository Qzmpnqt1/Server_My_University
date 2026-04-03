package org.example.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 3.3 не поддерживает spring.flyway.repair-on-migrate (свойства нет в FlywayProperties).
 * После правок уже применённых V__*.sql без repair Flyway падает на checksum mismatch и Tomcat не стартует.
 */
@Configuration
@ConditionalOnProperty(
        name = "app.flyway.repair-before-migrate",
        havingValue = "true",
        matchIfMissing = true
)
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
