package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.model.FinalAssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectInDirectionRequest {

    @NotNull
    private Long subjectId;

    @NotNull
    private Long directionId;

    @NotNull
    private Integer course;

    @NotNull
    private Integer semester;

    /** EXAM — оценка 2–5; CREDIT — только credit_status в grades */
    private FinalAssessmentType finalAssessmentType;
}
