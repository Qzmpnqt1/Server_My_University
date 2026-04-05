package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long subjectDirectionId;
    private String subjectName;
    private Integer grade;
    private Boolean creditStatus;
    /** EXAM — числовая итоговая оценка; CREDIT — зачёт/незачёт */
    private String finalAssessmentType;

    /** Курс и семестр дисциплины в направлении (для группировки в зачётке). */
    private Integer course;
    private Integer semester;

    /** Название направления подготовки. */
    private String directionName;

    /** Число практик, заведённых по дисциплине в направлении. */
    private Integer practiceCount;
}
