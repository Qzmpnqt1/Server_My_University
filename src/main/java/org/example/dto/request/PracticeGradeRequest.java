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
public class PracticeGradeRequest {

    @NotNull(message = "ID студента обязателен")
    private Long studentId;

    @NotNull(message = "ID практической работы обязателен")
    private Long practiceId;

    private Integer grade;

    private Boolean creditStatus;

    /** Если задано — студент должен состоять в этой группе. */
    private Long groupId;
}
