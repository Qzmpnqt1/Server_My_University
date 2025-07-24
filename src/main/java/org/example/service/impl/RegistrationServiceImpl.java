package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.ApiResponse;
import org.example.dto.RegistrationRequestDTO;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.RegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRequestRepository registrationRequestRepository;
    private final UsersRepository usersRepository;
    private final UniversityRepository universityRepository;
    private final InstituteRepository instituteRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SubjectRepository subjectRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ApiResponse<?> submitRegistrationRequest(RegistrationRequestDTO request) {
        // Check if email already exists
        if (usersRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Пользователь с таким email уже существует");
        }

        // Check if there is a pending registration request with this email
        if (registrationRequestRepository.existsByEmailAndStatus(request.getEmail(), RegistrationStatus.PENDING)) {
            return ApiResponse.error("Заявка на регистрацию с таким email уже существует и находится на рассмотрении");
        }

        // Check if university exists
        Optional<University> universityOptional = universityRepository.findById(request.getUniversityId());
        if (universityOptional.isEmpty()) {
            return ApiResponse.error("Указанный университет не найден");
        }

        University university = universityOptional.get();
        
        // Validate student-specific fields
        if (request.getUserType() == UserType.STUDENT) {
            if (request.getInstituteId() == null) {
                return ApiResponse.error("Необходимо указать институт");
            }
            if (request.getDirectionId() == null) {
                return ApiResponse.error("Необходимо указать направление обучения");
            }
            if (request.getGroupId() == null) {
                return ApiResponse.error("Необходимо указать группу");
            }
            if (request.getCourseYear() == null || request.getCourseYear() < 1 || request.getCourseYear() > 5) {
                return ApiResponse.error("Необходимо указать корректный курс обучения (1-5)");
            }

            // Verify that group exists
            Optional<AcademicGroup> groupOptional = academicGroupRepository.findById(request.getGroupId());
            if (groupOptional.isEmpty()) {
                return ApiResponse.error("Указанная группа не найдена");
            }
            
            // Verify course matches group
            AcademicGroup group = groupOptional.get();
            if (!group.getCourse().equals(request.getCourseYear())) {
                return ApiResponse.error("Выбранная группа не соответствует указанному курсу");
            }
            
            // Verify institute exists
            Optional<Institute> instituteOptional = instituteRepository.findById(request.getInstituteId());
            if (instituteOptional.isEmpty()) {
                return ApiResponse.error("Указанный институт не найден");
            }
        }

        // Validate teacher-specific fields
        Set<Subject> teacherSubjects = new HashSet<>();
        if (request.getUserType() == UserType.TEACHER) {
            if (request.getSubjectIds() == null || request.getSubjectIds().isEmpty()) {
                return ApiResponse.error("Необходимо указать хотя бы один преподаваемый предмет");
            }
            
            // Check if all subjects exist
            teacherSubjects = new HashSet<>(subjectRepository.findByIdIn(request.getSubjectIds()));
            if (teacherSubjects.size() != request.getSubjectIds().size()) {
                return ApiResponse.error("Один или несколько выбранных предметов не найдены");
            }
            
            // Verify institute exists for teacher
            if (request.getInstituteId() != null) {
                Optional<Institute> instituteOptional = instituteRepository.findById(request.getInstituteId());
                if (instituteOptional.isEmpty()) {
                    return ApiResponse.error("Указанный институт не найден");
                }
            }
        }

        // Создаем пользователя сразу без запроса на подтверждение
        Users newUser = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .userType(request.getUserType())
                .isActive(true) // Сразу активируем пользователя
                .build();
        
        // Сохраняем пользователя
        Users savedUser = usersRepository.save(newUser);
        
        // Создаем профиль в зависимости от типа пользователя
        if (request.getUserType() == UserType.STUDENT) {
            Optional<Institute> instituteOptional = instituteRepository.findById(request.getInstituteId());
            Optional<AcademicGroup> groupOptional = academicGroupRepository.findById(request.getGroupId());
            
            if (instituteOptional.isPresent() && groupOptional.isPresent()) {
                StudentProfile studentProfile = StudentProfile.builder()
                        .user(savedUser)
                        .institute(instituteOptional.get())
                        .group(groupOptional.get())
                        .build();
                
                studentProfileRepository.save(studentProfile);
            }
        } else if (request.getUserType() == UserType.TEACHER) {
            Optional<Institute> instituteOptional = Optional.empty();
            if (request.getInstituteId() != null) {
                instituteOptional = instituteRepository.findById(request.getInstituteId());
            }
            
            TeacherProfile teacherProfile = TeacherProfile.builder()
                    .user(savedUser)
                    .build();
                    
            if (instituteOptional.isPresent()) {
                teacherProfile.setInstitute(instituteOptional.get());
            }
            
            TeacherProfile savedTeacherProfile = teacherProfileRepository.save(teacherProfile);
            
            // Добавляем предметы преподавателю
            // Note: В зависимости от вашей модели данных, это может потребовать отдельной логики
            // teacherSubjectRepository.saveAll(...);
        }

        // Для истории можно все равно сохранить запрос на регистрацию, но сразу с подтвержденным статусом
        RegistrationRequest registrationRequest = RegistrationRequest.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .university(university)
                .userType(request.getUserType())
                .status(RegistrationStatus.APPROVED) // Сразу подтверждаем регистрацию
                .build();
                
        registrationRequestRepository.save(registrationRequest);

        return ApiResponse.success("Регистрация успешно выполнена. Вы можете войти в систему, используя указанные данные.", null);
    }
} 