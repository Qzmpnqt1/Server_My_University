package org.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseTestRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext context;
    private final Environment environment;

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public CommandLineRunner dbTestRunner() {
        return args -> {
            testDatabaseConnection();
            checkHibernateProperties();
        };
    }

    private void testDatabaseConnection() {
        try {
            log.info("Тестирование подключения к базе данных");
            
            Connection connection = dataSource.getConnection();
            boolean isValid = connection.isValid(5); // timeout 5 seconds
            log.info("Подключение к базе данных: {}", isValid ? "Успешно" : "Ошибка");
            
            if (isValid) {
                // Получаем метаданные
                DatabaseMetaData metaData = connection.getMetaData();
                String url = metaData.getURL();
                String dbName = connection.getCatalog();
                log.info("URL базы данных: {}", url);
                log.info("Имя базы данных: {}", dbName);
                
                // Проверяем таблицы
                List<String> tables = new ArrayList<>();
                try (ResultSet rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME"));
                    }
                }
                
                log.info("Таблицы в базе данных ({}):", tables.size());
                tables.forEach(table -> log.info(" - {}", table));
                
                // Выполняем простой запрос
                try {
                    jdbcTemplate.execute("SELECT 1");
                    log.info("Тестовый SQL-запрос выполнен успешно");
                } catch (Exception e) {
                    log.error("Ошибка при выполнении тестового SQL-запроса", e);
                }
            }
            
            connection.close();
        } catch (Exception e) {
            log.error("Ошибка при тестировании подключения к базе данных", e);
        }
    }
    
    private void checkHibernateProperties() {
        log.info("Проверка настроек Hibernate");
        
        log.info("spring.jpa.hibernate.ddl-auto: {}", 
                environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        log.info("spring.jpa.properties.hibernate.hbm2ddl.auto: {}", 
                environment.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto"));
        log.info("spring.jpa.generate-ddl: {}", 
                environment.getProperty("spring.jpa.generate-ddl"));
        log.info("spring.sql.init.mode: {}", 
                environment.getProperty("spring.sql.init.mode"));
    }
    
    @Transactional
    public void checkUserTable() {
        try {
            log.info("Проверка таблицы users");
            
            Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM users");
            Object result = query.getSingleResult();
            log.info("Количество записей в таблице users: {}", result);
        } catch (Exception e) {
            log.error("Ошибка при проверке таблицы users", e);
        }
    }
} 