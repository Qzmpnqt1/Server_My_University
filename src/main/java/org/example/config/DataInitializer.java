package org.example.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.UsersRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        try {
            log.info("Начинаем инициализацию тестовых данных");
            
            // Проверяем, есть ли уже пользователи в базе данных
            long userCount = usersRepository.count();
            log.info("Количество пользователей в БД: {}", userCount);
            
            // Если пользователей нет, создаем тестовых
            if (userCount == 0) {
                log.info("Создаем тестового пользователя");
                
                Users admin = Users.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Администратор")
                    .lastName("Системный")
                    .middleName("Тестович")
                    .userType(UserType.ADMIN)
                    .isActive(true)
                    .build();
                
                usersRepository.save(admin);
                log.info("Создан пользователь с ID: {}", admin.getId());
            }
            
            log.info("Инициализация тестовых данных завершена");
        } catch (Exception e) {
            log.error("Ошибка при инициализации тестовых данных", e);
        }
    }
} 