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
    private String subjectName;
    /** EXAM или CREDIT */
    private String assessmentType;
    private double averageGrade;
    private double medianGrade;
    private double creditRate;
    private int totalStudents;
    private int gradedStudents;
    private int missingValues;
    private Map<Integer, Long> gradeDistribution;
}
