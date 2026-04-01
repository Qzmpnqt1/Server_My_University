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
public class GroupStatisticsResponse {
    private Long groupId;
    private String groupName;
    private double averagePerformance;
    private double debtRate;
    private int studentCount;
    private int studentsWithDebt;
    private int countedValues;
    private long missingValues;
    private Map<String, Double> averageBySubject;
    /** Доля зачтённых (%) по зачётным предметам; ключ как в averageBySubject */
    private Map<String, Double> creditPassPercentBySubject;
}
