package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Полная замена набора назначений преподавателя за одну транзакцию.
 * Список — идентификаторы subjects_in_directions.id (без дубликатов на сервере).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSubjectReplaceRequest {

    @NotNull
    private List<Long> subjectDirectionIds;

    /**
     * Если задано и не совпадает с текущим числом назначений в БД — 409 (изменено параллельно).
     */
    private Integer expectedAssignmentCount;
}
