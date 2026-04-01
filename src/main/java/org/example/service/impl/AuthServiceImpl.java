package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.LoginRequest;
import org.example.dto.response.AuthResponse;
import org.example.exception.BadRequestException;
import org.example.model.Users;
import org.example.repository.UsersRepository;
import org.example.service.AuthService;
import org.example.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim();
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Неверный email или пароль"));

        if (!user.getIsActive()) {
            throw new BadRequestException("Аккаунт деактивирован");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный email или пароль");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .userType(user.getUserType())
                .build();
    }
}
