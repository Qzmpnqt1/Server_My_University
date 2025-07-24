package org.example.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class DatabaseConnectionChecker {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/db-check")
    public String checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            // Проверка соединения
            boolean isValid = connection.isValid(5); // timeout 5 seconds
            
            if (!isValid) {
                return "Ошибка подключения к базе данных";
            }
            
            // Получение метаданных соединения
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();
            String dbName = connection.getCatalog();
            
            // Проверка таблиц - только для текущей базы данных
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            
            // Проверка структуры таблицы users, если она существует
            boolean usersTableExists = tables.contains("users");
            List<String> usersColumns = new ArrayList<>();
            if (usersTableExists) {
                try (ResultSet rs = metaData.getColumns(dbName, null, "users", "%")) {
                    while (rs.next()) {
                        usersColumns.add(rs.getString("COLUMN_NAME"));
                    }
                }
            }
            
            // Проверка простым SQL запросом
            try {
                jdbcTemplate.queryForObject("SELECT 1 FROM dual", Integer.class);
                
                StringBuilder result = new StringBuilder("Подключение успешно\n");
                result.append("URL: ").append(url).append("\n");
                result.append("База данных: ").append(dbName).append("\n");
                result.append("Найдено таблиц: ").append(tables.size()).append("\n");
                
                if (!tables.isEmpty()) {
                    result.append("Список таблиц: ").append(String.join(", ", tables)).append("\n\n");
                } else {
                    result.append("В базе данных нет таблиц\n\n");
                }
                
                // Добавляем информацию о таблице users
                if (usersTableExists) {
                    result.append("Таблица users существует\n");
                    result.append("Колонки таблицы users: ").append(String.join(", ", usersColumns));
                } else {
                    result.append("Таблица users НЕ существует");
                }
                
                return result.toString();
                
            } catch (Exception e) {
                log.error("Ошибка выполнения тестового запроса: {}", e.getMessage());
                return "Ошибка выполнения тестового запроса: " + e.getMessage();
            }
            
        } catch (Exception e) {
            log.error("Ошибка подключения к базе данных", e);
            return "Ошибка подключения к базе данных: " + e.getMessage();
        }
    }
} 