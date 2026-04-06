package org.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.UserType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminAccountRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;

    @NotBlank(message = "Имя обязательно")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    private String lastName;

    private String middleName;

    /**
     * Только {@link UserType#ADMIN} или {@link UserType#SUPER_ADMIN}.
     * Для ADMIN обязателен {@link #universityId}; для SUPER_ADMIN должен быть {@code null}.
     */
    @NotNull(message = "Тип учётной записи обязателен")
    private UserType userType;

    /** Обязателен при {@code userType == ADMIN}. */
    private Long universityId;
}
