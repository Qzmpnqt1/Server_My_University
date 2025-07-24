package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.ApiResponse;
import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.dto.RegistrationRequestDTO;
import org.example.model.Users;
import org.example.repository.UsersRepository;
import org.example.service.AuthService;
import org.example.service.JwtService;
import org.example.service.RegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationService registrationService;

    @Override
    public ApiResponse<AuthResponse> login(AuthRequest request) {
        // Найти пользователя по email
        Users user = usersRepository.findByEmail(request.getEmail())
                .orElse(null);
                
        if (user == null) {
            return ApiResponse.error("Пользователь с таким email не найден");
        }
        
        // Проверить активен ли пользователь
        if (!user.getIsActive()) {
            return ApiResponse.error("Ваш аккаунт деактивирован. Свяжитесь с администрацией");
        }
        
        // Проверить пароль
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.error("Неверный пароль");
        }
        
        // Сгенерировать JWT токен
        String token = jwtService.generateToken(user);
        
        // Создать ответ
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .userType(user.getUserType())
                .build();
                
        return ApiResponse.success("Вход выполнен успешно", response);
    }

    @Override
    public ApiResponse<Void> register(RegistrationRequestDTO request) {
        // Теперь регистрация происходит автоматически без подтверждения администратора
        ApiResponse<?> response = registrationService.submitRegistrationRequest(request);
        
        // Если регистрация успешна, добавляем информацию о возможности входа
        if (response.isSuccess()) {
            return new ApiResponse<>(true, "Регистрация успешно выполнена. Теперь вы можете войти в систему, используя указанные учетные данные.", null);
        }
        
        // Если возникла ошибка, просто передаем её сообщение
        return new ApiResponse<>(false, response.getMessage(), null);
    }
} 