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
public class ScheduleStatisticsResponse {
    private String scope;
    private Long entityId;
    /** Если null — статистика по всем неделям (прежнее поведение). */
    private Integer weekNumberFilter;
    private int totalLessons;
    private double totalHours;
    private Map<Integer, Long> byDayOfWeek;
    private Map<Integer, Long> byWeekNumber;
}
