package org.example.integration;

import org.example.repository.UsersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Flyway-сиды используют BCrypt из SQL; текущий {@link PasswordEncoder} приложения должен совпадать.
 * Нормализуем известные тестовые учётки до пароля {@code Admin123!} до прогона IT.
 */
@TestConfiguration
@Profile("integrationtest")
public class IntegrationTestAccountNormalizer {

    private static final String KNOWN_IT_PASSWORD = "Admin123!";

    @Bean
    CommandLineRunner integrationTestNormalizeSeededPasswords(
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            for (String email : new String[]{
                    "superadmin@moyvuz.local",
                    "admin@university.ru",
                    "teacher.itest@moyvuz.local",
            }) {
                usersRepository.findByEmail(email).ifPresent(u -> {
                    if (!passwordEncoder.matches(KNOWN_IT_PASSWORD, u.getPasswordHash())) {
                        u.setPasswordHash(passwordEncoder.encode(KNOWN_IT_PASSWORD));
                        usersRepository.save(u);
                    }
                });
            }
        };
    }
}
