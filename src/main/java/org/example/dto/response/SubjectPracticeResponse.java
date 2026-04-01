package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPracticeResponse {

    private Long id;
    private Long subjectDirectionId;
    private Integer practiceNumber;
    private String practiceTitle;
    private Integer maxGrade;
    private Boolean isCredit;
}
