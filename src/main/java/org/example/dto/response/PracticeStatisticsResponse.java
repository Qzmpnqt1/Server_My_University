package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Агрегаты по практикам дисциплины (привязка к subject_direction_id).
 * <p>{@code overallProgress} и {@code completionPercentage} — одно и то же значение в диапазоне 0–100
 * (доля практик, по которым у студентов есть итоговый результат); поля продублированы для совместимости клиентов.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeStatisticsResponse {
    private Long subjectDirectionId;
    private Long directionId;
    private Long groupIdFilter;
    private String samplingScope;
    /** Число студентов в области выборки (знаменатель для fill/credit по практикам). */
    private int totalRequiredStudents;
    private String subjectName;
    private double overallProgress;
    /** Среднее сырого балла по оценочным практикам (по одной средней на практику). */
    private double totalScoreAverage;
    private Double averageNormalizedPercentAcrossNumericPractices;
    private double completionPercentage;
    private int totalPractices;
    private int countedValues;
    private int missingValues;
    private List<PracticeDetail> practices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PracticeDetail {
        private Long practiceId;
        private int practiceNumber;
        private String practiceTitle;
        /** Число строк в practice_grades (для контроля). */
        private int totalRecords;
        private int totalRequiredStudents;
        private int withResult;
        private double completionRate;
        private double averageGrade;
        private Double creditRate;
        /** Перевод среднего в шкалу 0–5 (совместимость UI при положительном max_grade). */
        private Double normalizedAverage;
        /** Среднее (grade/max_grade)*100 по валидным оценкам (2.4). */
        private Double averageNormalizedPercent;
    }
}
