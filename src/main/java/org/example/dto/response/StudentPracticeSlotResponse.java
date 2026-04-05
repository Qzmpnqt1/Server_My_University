package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Слот практики в зачётке студента: каркас из учебного плана + фактическая оценка, если есть.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPracticeSlotResponse {

    private Long practiceId;
    private Integer practiceNumber;
    private String practiceTitle;
    private Integer maxGrade;
    private Boolean isCredit;
    private Integer grade;
    private Boolean creditStatus;
    /** Есть ли выставленный итог по правилам типа практики (зачёт / числовая оценка в допустимом диапазоне). */
    private boolean hasResult;
}
