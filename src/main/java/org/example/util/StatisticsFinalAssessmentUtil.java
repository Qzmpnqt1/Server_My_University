package org.example.util;

import org.example.model.FinalAssessmentType;
import org.example.model.Grade;
import org.example.model.PracticeGrade;
import org.example.model.SubjectInDirection;
import org.example.model.SubjectPractice;

/**
 * Единая логика «итог по дисциплине» для статистики: экзамен (2–5) и зачёт (credit_status).
 */
public final class StatisticsFinalAssessmentUtil {

    private StatisticsFinalAssessmentUtil() {
    }

    /**
     * Согласовано с {@link org.example.service.impl.PracticeGradeServiceImpl#validateGrade}:
     * при положительном max_grade допустимо 0..max_grade, иначе 2..5.
     */
    public static boolean isValidNumericPracticeGrade(Integer grade, Integer maxGrade) {
        if (grade == null) {
            return false;
        }
        if (maxGrade != null && maxGrade > 0) {
            return grade >= 0 && grade <= maxGrade;
        }
        return grade >= 2 && grade <= 5;
    }

    public static boolean practiceResultComplete(PracticeGrade pg, SubjectPractice practice) {
        if (Boolean.TRUE.equals(practice.getIsCredit())) {
            return pg != null && pg.getCreditStatus() != null;
        }
        return pg != null && isValidNumericPracticeGrade(pg.getGrade(), practice.getMaxGrade());
    }

    /** norm_i = (grade / max_grade) * 100; при отсутствии max возвращает NaN. */
    public static double normPercentOfPracticeGrade(int grade, Integer maxGrade) {
        if (maxGrade == null || maxGrade <= 0) {
            return Double.NaN;
        }
        return 100.0 * grade / maxGrade;
    }

    public static FinalAssessmentType effectiveType(SubjectInDirection s) {
        return s.getFinalAssessmentType() != null ? s.getFinalAssessmentType() : FinalAssessmentType.EXAM;
    }

    /** Итог выставлен (для заполненности ячеек и missingValues). */
    public static boolean isFinalComplete(Grade g, FinalAssessmentType at) {
        if (g == null) {
            return false;
        }
        if (at == FinalAssessmentType.CREDIT) {
            return g.getCreditStatus() != null;
        }
        Integer gr = g.getGrade();
        return gr != null && gr >= 2 && gr <= 5;
    }

    /** Задолженность по одной дисциплине для студента. */
    public static boolean isDebtForSubject(Grade g, FinalAssessmentType at) {
        if (g == null) {
            return true;
        }
        if (at == FinalAssessmentType.CREDIT) {
            if (g.getCreditStatus() == null) {
                return true;
            }
            return !Boolean.TRUE.equals(g.getCreditStatus());
        }
        Integer gr = g.getGrade();
        if (gr == null || gr < 2 || gr > 5) {
            return true;
        }
        return gr <= 2;
    }

    /**
     * Переводит средний балл практики в шкалу 0–5 по max_grade.
     * @return null если нормализация неприменима (нет положительного max)
     */
    public static Double normalizedAverageToFivePoint(double rawAverage, Integer maxGrade) {
        if (maxGrade == null || maxGrade <= 0) {
            return null;
        }
        double v = rawAverage * 5.0 / maxGrade;
        if (v < 0) {
            v = 0;
        }
        if (v > 5) {
            v = 5;
        }
        return v;
    }
}
