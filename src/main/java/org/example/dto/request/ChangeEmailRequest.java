package org.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailRequest {

    @NotBlank(message = "Новый email обязателен")
    @Email(message = "Некорректный формат email")
    private String newEmail;

    @NotBlank(message = "Пароль обязателен для подтверждения смены email")
    private String currentPassword;
}
