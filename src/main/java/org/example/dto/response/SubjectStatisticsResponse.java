package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatisticsResponse {
    private Long subjectDirectionId;
    private Long directionId;
    /** Переданный фильтр группы или null (вся выборка по направлению). */
    private Long groupIdFilter;
    /**
     * GROUP — только студенты группы; DIRECTION_ALL_GROUPS — все студенты групп направления дисциплины.
     */
    private String samplingScope;
    /** Для EXAM: среднее/медиана только по итоговым оценкам 2–5. */
    private String averagePerformanceScope;
    private String subjectName;
    /** EXAM или CREDIT */
    private String assessmentType;
    private double averageGrade;
    private double medianGrade;
    private double creditRate;
    /** Все студенты, обязанные изучать дисциплину в выбранной области. */
    private int totalStudents;
    private int gradedStudents;
    private int missingValues;
    private Map<Integer, Long> gradeDistribution;
}
