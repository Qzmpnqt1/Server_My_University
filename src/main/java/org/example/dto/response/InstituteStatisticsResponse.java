package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstituteStatisticsResponse {
    private Long instituteId;
    private String instituteName;
    private double averagePerformance;
    private double debtRate;
    private int totalStudents;
    private int studentsWithDebt;
    private int directionCount;
    private List<DirectionSummary> directions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionSummary {
        private Long directionId;
        private String directionName;
        private double averagePerformance;
        private int studentCount;
    }
}
