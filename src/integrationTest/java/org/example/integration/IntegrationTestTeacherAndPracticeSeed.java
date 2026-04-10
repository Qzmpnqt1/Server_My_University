package org.example.integration;

import org.example.model.Institute;
import org.example.model.SubjectInDirection;
import org.example.model.SubjectPractice;
import org.example.model.TeacherProfile;
import org.example.model.TeacherSubject;
import org.example.model.University;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.InstituteRepository;
import org.example.repository.SubjectInDirectionRepository;
import org.example.repository.SubjectPracticeRepository;
import org.example.repository.TeacherProfileRepository;
import org.example.repository.TeacherSubjectRepository;
import org.example.repository.UniversityRepository;
import org.example.repository.UsersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Сид преподавателя и практики для сквозных IT (без spring.sql.init — избегаем цикла Flyway/EMF).
 */
@TestConfiguration
@Profile("integrationtest")
public class IntegrationTestTeacherAndPracticeSeed {

    public static final String TEACHER_EMAIL = "teacher.itest@moyvuz.local";
    private static final String IT_PASSWORD = "Admin123!";
    private static final int PRACTICE_NUMBER_ITEST = 99;

    @Bean
    CommandLineRunner integrationTestSeedTeacherAndPractice(
            UsersRepository usersRepository,
            TeacherProfileRepository teacherProfileRepository,
            TeacherSubjectRepository teacherSubjectRepository,
            SubjectInDirectionRepository subjectInDirectionRepository,
            SubjectPracticeRepository subjectPracticeRepository,
            UniversityRepository universityRepository,
            InstituteRepository instituteRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            University uni = universityRepository.findById(1L).orElse(null);
            Institute inst = instituteRepository.findById(1L).orElse(null);
            SubjectInDirection sid = subjectInDirectionRepository.findById(1L).orElse(null);
            if (uni == null || inst == null || sid == null) {
                return;
            }

            Users teacher = usersRepository.findByEmail(TEACHER_EMAIL).orElseGet(() -> {
                Users u = Users.builder()
                        .email(TEACHER_EMAIL)
                        .passwordHash(passwordEncoder.encode(IT_PASSWORD))
                        .firstName("ИТ")
                        .lastName("Преподаватель")
                        .userType(UserType.TEACHER)
                        .isActive(true)
                        .build();
                return usersRepository.save(u);
            });

            TeacherProfile tp = teacherProfileRepository.findByUserId(teacher.getId()).orElseGet(() ->
                    teacherProfileRepository.save(TeacherProfile.builder()
                            .user(teacher)
                            .university(uni)
                            .institute(inst)
                            .build()));

            if (!teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(tp.getId(), sid.getId())) {
                teacherSubjectRepository.save(TeacherSubject.builder()
                        .teacher(tp)
                        .subjectInDirection(sid)
                        .build());
            }

            boolean hasItestPractice = subjectPracticeRepository.findBySubjectDirectionId(sid.getId()).stream()
                    .anyMatch(p -> p.getPracticeNumber() != null && p.getPracticeNumber() == PRACTICE_NUMBER_ITEST);
            if (!hasItestPractice) {
                subjectPracticeRepository.save(SubjectPractice.builder()
                        .subjectDirection(sid)
                        .practiceNumber(PRACTICE_NUMBER_ITEST)
                        .practiceTitle("ITest практика")
                        .maxGrade(5)
                        .isCredit(false)
                        .build());
            }
        };
    }
}
