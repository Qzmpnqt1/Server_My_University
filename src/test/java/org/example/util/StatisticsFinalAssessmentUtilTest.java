package org.example.util;

import org.example.model.FinalAssessmentType;
import org.example.model.Grade;
import org.example.model.SubjectInDirection;
import org.example.model.Subject;
import org.example.model.StudyDirection;
import org.example.model.Institute;
import org.example.model.University;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsFinalAssessmentUtilTest {

    private final Subject subj = Subject.builder().id(1L).name("X").build();
    private final University uni = University.builder().id(1L).name("U").build();
    private final Institute inst = Institute.builder().id(1L).name("I").university(uni).build();
    private final StudyDirection dir = StudyDirection.builder().id(1L).name("D").institute(inst).build();

    @Test
    void effectiveTypeDefaultsToExam() {
        SubjectInDirection s = SubjectInDirection.builder()
                .id(1L).subject(subj).direction(dir).course(1).semester(1)
                .finalAssessmentType(null)
                .build();
        assertEquals(FinalAssessmentType.EXAM, StatisticsFinalAssessmentUtil.effectiveType(s));
    }

    @Test
    void creditCompleteOnlyWhenStatusSet() {
        assertFalse(StatisticsFinalAssessmentUtil.isFinalComplete(null, FinalAssessmentType.CREDIT));
        Grade g = Grade.builder().creditStatus(null).build();
        assertFalse(StatisticsFinalAssessmentUtil.isFinalComplete(g, FinalAssessmentType.CREDIT));
        g.setCreditStatus(true);
        assertTrue(StatisticsFinalAssessmentUtil.isFinalComplete(g, FinalAssessmentType.CREDIT));
    }

    @Test
    void examCompleteOnlyInRange2to5() {
        assertFalse(StatisticsFinalAssessmentUtil.isFinalComplete(
                Grade.builder().grade(null).build(), FinalAssessmentType.EXAM));
        assertFalse(StatisticsFinalAssessmentUtil.isFinalComplete(
                Grade.builder().grade(1).build(), FinalAssessmentType.EXAM));
        assertTrue(StatisticsFinalAssessmentUtil.isFinalComplete(
                Grade.builder().grade(3).build(), FinalAssessmentType.EXAM));
    }

    @Test
    void debtCreditFailsWhenFalse() {
        assertTrue(StatisticsFinalAssessmentUtil.isDebtForSubject(
                Grade.builder().creditStatus(false).build(), FinalAssessmentType.CREDIT));
        assertFalse(StatisticsFinalAssessmentUtil.isDebtForSubject(
                Grade.builder().creditStatus(true).build(), FinalAssessmentType.CREDIT));
    }

    @Test
    void debtExamWhenGrade2() {
        assertTrue(StatisticsFinalAssessmentUtil.isDebtForSubject(
                Grade.builder().grade(2).build(), FinalAssessmentType.EXAM));
        assertFalse(StatisticsFinalAssessmentUtil.isDebtForSubject(
                Grade.builder().grade(3).build(), FinalAssessmentType.EXAM));
    }

    @Test
    void normalizeToFivePoint() {
        assertNull(StatisticsFinalAssessmentUtil.normalizedAverageToFivePoint(4.5, null));
        assertNull(StatisticsFinalAssessmentUtil.normalizedAverageToFivePoint(4.5, 0));
        assertEquals(2.25, StatisticsFinalAssessmentUtil.normalizedAverageToFivePoint(4.5, 10), 0.001);
        assertEquals(5.0, StatisticsFinalAssessmentUtil.normalizedAverageToFivePoint(10, 10), 0.001);
    }
}
