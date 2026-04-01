package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicGroupResponse {

    private Long id;
    private String name;
    private Integer course;
    private Integer yearOfAdmission;
    private Long directionId;
    private String directionName;
}
