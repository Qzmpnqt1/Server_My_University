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
public class TeacherJournalResponse {

    private Long subjectDirectionId;
    private String subjectName;
    private List<StudentRow> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentRow {
        private Long studentUserId;
        private String studentName;
        private String groupName;
        private GradeResponse finalGrade;
        private List<PracticeGradeResponse> practiceGrades;
    }
}
