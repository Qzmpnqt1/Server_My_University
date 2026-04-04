package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Практика в контексте оценивания студента (с текущим значением, если есть). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPracticeSlotResponse {

    private Long practiceId;
    private Integer practiceNumber;
    private String practiceTitle;
    private Boolean creditPractice;
    private Integer maxGrade;
    /** id строки practice_grades, если оценка уже сохранена */
    private Long gradeRowId;
    private Integer grade;
    private Boolean creditStatus;
}
