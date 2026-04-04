package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCompareClassroomOptionResponse {
    private Long id;
    private String building;
    private String roomNumber;
    private Integer capacity;
    /** Удобная подпись, например «Корпус А, ауд. 305». */
    private String label;
}
