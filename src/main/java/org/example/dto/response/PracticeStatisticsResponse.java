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
    private String subjectName;
    private double overallProgress;
    private double totalScoreAverage;
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
        private int totalRecords;
        private int withResult;
        private double completionRate;
        private double averageGrade;
        private Double creditRate;
        private Double normalizedAverage;
    }
}
