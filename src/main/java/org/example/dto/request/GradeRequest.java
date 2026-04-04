package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRequest {

    @NotNull(message = "ID студента обязателен")
    private Long studentId;

    @NotNull(message = "ID предмета в направлении обязателен")
    private Long subjectDirectionId;

    private Integer grade;

    private Boolean creditStatus;

    /** Если задано — студент должен состоять в этой группе (усиление проверки при каскадном UI). */
    private Long groupId;
}
