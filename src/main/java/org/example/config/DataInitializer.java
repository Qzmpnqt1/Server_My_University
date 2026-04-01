package org.example.config;

import lombok.RequiredArgsConstructor;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usersRepository.findByEmail("admin@university.ru").isPresent()) {
            Users admin = usersRepository.findByEmail("admin@university.ru").get();
            if (!passwordEncoder.matches("Admin123!", admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
                usersRepository.save(admin);
                log.info("Admin password has been reset to the default");
            }
            return;
        }
        log.info("No admin user found — the Flyway seed should have created one");
    }
}
