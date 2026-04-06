package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "ID получателя обязателен")
    private Long recipientId;

    @NotBlank(message = "Текст сообщения обязателен")
    private String text;

    /**
     * Для {@code SUPER_ADMIN}: null в глобальном режиме — можно писать любому активному пользователю;
     * non-null — получатель должен относиться к этому вузу.
     * Для остальных ролей игнорируется.
     */
    private Long scopeUniversityId;
}
