package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Прогресс по практикам в рамках одной дисциплины в учебном плане (subject_direction).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPracticeProgressItem {
    private Long subjectDirectionId;
    private String subjectName;
    private Integer course;
    private Integer semester;
    private int totalPractices;
    private int practicesWithResult;
    private double practiceProgressPercent;
    /** Сумма числовых оценок по оценочным практикам; null если таких практик/оценок нет. */
    private Integer sumNumericPracticePoints;
}
