package org.example.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HibernateDebugConfig {

    private final DataSource dataSource;
    private final LocalContainerEntityManagerFactoryBean entityManagerFactory;

    @PostConstruct
    public void debugHibernate() {
        try {
            log.info("Проверка настроек Hibernate");
            log.info("DataSource: {}", dataSource.getClass().getName());
            
            // Вывод списка сущностей, обнаруженных Hibernate
            SessionFactoryImplementor sessionFactory = entityManagerFactory.getNativeEntityManagerFactory()
                    .unwrap(SessionFactoryImplementor.class);
            
            log.info("Список сконфигурированных сущностей:");
            sessionFactory.getMetamodel().getEntities().forEach(entityType -> {
                log.info(" - {} (таблица: {})", entityType.getJavaType().getName(), 
                        entityType.getName());
            });

            // Проверка доступа к базе данных
            try {
                boolean isValid = dataSource.getConnection().isValid(5);
                log.info("Подключение к базе данных: {}", isValid ? "Успешно" : "Ошибка");
            } catch (Exception e) {
                log.error("Ошибка при проверке соединения с БД", e);
            }

        } catch (Exception e) {
            log.error("Ошибка при отладке Hibernate", e);
        }
    }
} 