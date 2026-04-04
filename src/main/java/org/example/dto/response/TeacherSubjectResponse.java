package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSubjectResponse {

    private Long id;
    private Long teacherId;
    private String teacherName;
    /** Идентификатор связки предмета с направлением (subjects_in_directions.id). */
    private Long subjectDirectionId;
    /** id предмета (справочник) — для отображения и совместимости. */
    private Long subjectId;
    private String subjectName;
    private Long directionId;
    private String directionName;
    private Long instituteId;
    private String instituteName;
    private Integer course;
    private Integer semester;
}
