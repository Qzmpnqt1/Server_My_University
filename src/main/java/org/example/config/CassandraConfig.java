package org.example.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
@ConditionalOnProperty(name = "spring.cassandra.contact-points")
public class CassandraConfig {

    private static final Logger log = LoggerFactory.getLogger(CassandraConfig.class);

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Value("${spring.cassandra.keyspace-name:my_university}")
    private String keyspaceName;

    /** Повторы при старте: Cassandra / init keyspace иногда ещё не готовы, иначе сессия остаётся null до рестарта приложения. */
    @Value("${app.cassandra.connect-max-attempts:40}")
    private int connectMaxAttempts;

    @Value("${app.cassandra.connect-delay-ms:2000}")
    private long connectDelayMs;

    @Bean
    public CqlSession cqlSession() {
        log.info("Connecting to Cassandra at {}:{}, datacenter={}, keyspace={} (max {} attempts, delay {} ms)",
                contactPoints, port, localDatacenter, keyspaceName, connectMaxAttempts, connectDelayMs);

        Exception last = null;
        for (int attempt = 1; attempt <= connectMaxAttempts; attempt++) {
            try {
                CqlSession session = CqlSession.builder()
                        .addContactPoint(new InetSocketAddress(contactPoints, port))
                        .withLocalDatacenter(localDatacenter)
                        .withKeyspace(keyspaceName)
                        .build();
                log.info("Successfully connected to Cassandra (attempt {}/{})", attempt, connectMaxAttempts);
                return session;
            } catch (Exception e) {
                last = e;
                log.warn("Cassandra not ready yet (attempt {}/{}): {} — {}",
                        attempt, connectMaxAttempts, e.getClass().getSimpleName(), e.getMessage());
                if (attempt < connectMaxAttempts) {
                    try {
                        Thread.sleep(connectDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Cassandra connect retry interrupted");
                        break;
                    }
                }
            }
        }
        log.warn("Failed to connect to Cassandra after {} attempts. Chat will stay disabled until the app is restarted. Last error: {}",
                connectMaxAttempts, last != null ? last.getMessage() : "unknown");
        return null;
    }
}
