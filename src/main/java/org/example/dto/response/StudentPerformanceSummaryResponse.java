package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    private List<SubjectPracticeProgressItem> subjectPracticeProgressByDiscipline = new ArrayList<>();
}
