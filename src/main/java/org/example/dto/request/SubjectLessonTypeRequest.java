package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.LessonType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectLessonTypeRequest {

    @NotNull
    private Long subjectDirectionId;

    @NotNull
    private LessonType lessonType;
}
