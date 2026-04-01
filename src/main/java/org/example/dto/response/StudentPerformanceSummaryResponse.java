package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPerformanceSummaryResponse {

    private Integer courseFilter;
    private Integer semesterFilter;

    private int plannedSubjects;
    private int subjectsWithFinalResult;
    private int subjectsCredited;
    private Double averageNumericGrade;

    private int totalPractices;
    private int practicesWithResult;

    private double subjectCompletionPercent;
    private double practiceCompletionPercent;
}
