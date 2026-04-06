package org.example.config;

import lombok.RequiredArgsConstructor;
import org.example.model.AdminProfile;
import org.example.model.University;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.AdminProfileRepository;
import org.example.repository.UniversityRepository;
import org.example.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEFAULT_ADMIN_EMAIL = "admin@university.ru";
    private static final String DEFAULT_DEV_PASSWORD = "Admin123!";
    private static final String DOCKER_SUPER_EMAIL = "superadmin@moyvuz.local";

    private final UsersRepository usersRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final UniversityRepository universityRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void run(String... args) {
        if (usersRepository.findByEmail(DEFAULT_ADMIN_EMAIL).isPresent()) {
            Users admin = usersRepository.findByEmail(DEFAULT_ADMIN_EMAIL).orElseThrow();
            if (!passwordEncoder.matches(DEFAULT_DEV_PASSWORD, admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(DEFAULT_DEV_PASSWORD));
                usersRepository.save(admin);
                log.info("Admin password has been reset to the default");
            }
        } else {
            seedCampusAdminIfPossible();
        }

        if (isDockerProfile()) {
            ensureDockerSuperAdminPassword();
        }
    }

    private void seedCampusAdminIfPossible() {
        University uni = universityRepository.findAll().stream()
                .min(Comparator.comparing(University::getId))
                .orElse(null);
        if (uni == null) {
            log.warn("No admin user and no universities in DB — add V15 seed or create a university, then restart");
            return;
        }
        Users admin = Users.builder()
                .email(DEFAULT_ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(DEFAULT_DEV_PASSWORD))
                .firstName("Системный")
                .lastName("Администратор")
                .userType(UserType.ADMIN)
                .isActive(true)
                .build();
        Users saved = usersRepository.save(admin);
        adminProfileRepository.save(AdminProfile.builder()
                .user(saved)
                .university(uni)
                .role("ADMIN")
                .build());
        log.info("Created missing {} linked to university id={} (password: {})",
                DEFAULT_ADMIN_EMAIL, uni.getId(), DEFAULT_DEV_PASSWORD);
    }

    /**
     * В docker-профиле перекодируем пароль супер-админа приложением, если он не совпадает с дефолтным —
     * так исправляются «битые» BCrypt из SQL/миграций, которые Spring не принимает.
     */
    private void ensureDockerSuperAdminPassword() {
        usersRepository.findByEmail(DOCKER_SUPER_EMAIL).ifPresent(u -> {
            if (!passwordEncoder.matches(DEFAULT_DEV_PASSWORD, u.getPasswordHash())) {
                u.setPasswordHash(passwordEncoder.encode(DEFAULT_DEV_PASSWORD));
                usersRepository.save(u);
                log.warn("{}: пароль не совпадал с {}, сохранён новый BCrypt из приложения",
                        DOCKER_SUPER_EMAIL, DEFAULT_DEV_PASSWORD);
            }
        });
    }

    private boolean isDockerProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("docker");
    }
}
