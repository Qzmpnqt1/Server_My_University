package org.example.dto.response;

import org.example.model.FinalAssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectInDirectionResponse {

    private Long id;
    private Long subjectId;
    private String subjectName;
    private Long directionId;
    private String directionName;
    private Integer course;
    private Integer semester;
    private FinalAssessmentType finalAssessmentType;
}
