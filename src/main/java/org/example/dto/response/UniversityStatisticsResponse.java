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
public class UniversityStatisticsResponse {
    private Long universityId;
    private String universityName;
    private String averagePerformanceScope;
    private double averagePerformance;
    private double debtRate;
    private int totalStudents;
    private int studentsWithDebt;
    private int instituteCount;
    private List<InstituteSummary> institutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstituteSummary {
        private Long instituteId;
        private String instituteName;
        private double averagePerformance;
        private int studentCount;
    }
}
