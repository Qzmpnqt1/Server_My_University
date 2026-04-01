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

    @Bean
    public CqlSession cqlSession() {
        try {
            log.info("Connecting to Cassandra at {}:{}, datacenter={}, keyspace={}",
                    contactPoints, port, localDatacenter, keyspaceName);

            CqlSession session = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(contactPoints, port))
                    .withLocalDatacenter(localDatacenter)
                    .withKeyspace(keyspaceName)
                    .build();

            log.info("Successfully connected to Cassandra");
            return session;
        } catch (Exception e) {
            log.warn("Failed to connect to Cassandra: {}. Chat features will be unavailable until Cassandra is reachable.",
                    e.getMessage());
            return null;
        }
    }
}
