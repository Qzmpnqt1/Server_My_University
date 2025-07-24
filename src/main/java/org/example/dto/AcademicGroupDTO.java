package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicGroupDTO {
    private Integer id;
    private String name;
    private Integer course;
    private Integer yearOfAdmission;
    private Integer directionId;
} 