package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeGradeResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long practiceId;
    private String practiceTitle;
    private Integer practiceNumber;
    private Integer grade;
    private Boolean creditStatus;
    private Integer maxGrade;
    private Boolean practiceIsCredit;
}
