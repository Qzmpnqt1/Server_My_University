package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.cassandra.CassandraHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;

@Configuration
@EnableAutoConfiguration(exclude = {
    CassandraAutoConfiguration.class,
    CassandraHealthContributorAutoConfiguration.class,
    CassandraDataAutoConfiguration.class,
    CassandraReactiveDataAutoConfiguration.class,
    CassandraReactiveRepositoriesAutoConfiguration.class,
    CassandraRepositoriesAutoConfiguration.class
})
public class ApplicationConfig {
    // Пока пустая конфигурация
    // Здесь можно будет добавить другие настройки приложения
} 