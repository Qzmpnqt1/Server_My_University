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
public class DirectionStatisticsResponse {
    private Long directionId;
    private String directionName;
    private String averagePerformanceScope;
    private double averagePerformance;
    private double debtRate;
    private int totalStudents;
    private int studentsWithDebt;
    private int groupCount;
    private List<GroupSummary> groups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSummary {
        private Long groupId;
        private String groupName;
        private double averagePerformance;
        private double debtRate;
        private int studentCount;
    }
}
