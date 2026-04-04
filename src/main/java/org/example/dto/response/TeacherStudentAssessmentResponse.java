package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStudentAssessmentResponse {

    private Long subjectDirectionId;
    private Long directionId;
    private Long instituteId;
    private Long groupId;
    private Long studentUserId;

    private String instituteName;
    private String directionName;
    private String subjectName;
    private String groupName;
    private String studentDisplayName;

    private String finalAssessmentType;

    private SubjectInDirectionResponse subjectInDirection;

    private GradeResponse finalGrade;

    private List<TeacherPracticeSlotResponse> practices;
}
