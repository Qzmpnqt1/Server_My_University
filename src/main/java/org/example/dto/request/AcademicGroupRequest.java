package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicGroupRequest {

    @NotBlank
    private String name;

    @NotNull
    private Integer course;

    @NotNull
    private Integer yearOfAdmission;

    @NotNull
    private Long directionId;
}
