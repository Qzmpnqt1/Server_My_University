package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {
    @NotBlank(message = "Фамилия обязательна")
    private String lastName;

    @NotBlank(message = "Имя обязательно")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Неверный формат email")
    @Pattern(regexp = ".*@gmail\\.com$", message = "Email должен заканчиваться на @gmail.com")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать не менее 6 символов")
    private String password;

    @NotNull(message = "Тип пользователя обязателен")
    private UserType userType;

    @NotNull(message = "ID университета обязателен")
    private Integer universityId;

    // Поля для студента
    private Integer courseYear;
    private Integer instituteId;
    private Integer directionId;
    private Integer groupId;

    // Поля для преподавателя
    private List<Integer> subjectIds;
} 