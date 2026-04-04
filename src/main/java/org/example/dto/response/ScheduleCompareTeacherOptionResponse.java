package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCompareTeacherOptionResponse {
    private Long userId;
    private String displayName;
    private String instituteName;
    private String position;
}
