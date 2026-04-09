package org.example.util;

import org.example.exception.BadRequestException;
import org.example.model.AcademicGroup;
import org.example.model.StudentProfile;
import org.example.model.SubjectInDirection;

import java.util.Objects;

/**
 * Единое бизнес-правило: оценки и практики допустимы только если курс группы студента совпадает с курсом
 * дисциплины в учебном плане (и направление совпадает).
 */
public final class CourseConsistency {

    private CourseConsistency() {
    }

    public static void assertGroupMatchesSubjectDirection(AcademicGroup group, SubjectInDirection sid) {
        if (!Objects.equals(group.getDirection().getId(), sid.getDirection().getId())) {
            throw new BadRequestException("Группа не относится к направлению выбранной дисциплины");
        }
        assertCourseMatches(group, sid);
    }

    public static void assertStudentProfileMatchesSubjectDirection(StudentProfile sp, SubjectInDirection sid) {
        if (!Objects.equals(sp.getGroup().getDirection().getId(), sid.getDirection().getId())) {
            throw new BadRequestException("Студент не обучается на направлении выбранной дисциплины");
        }
        assertCourseMatches(sp.getGroup(), sid);
    }

    private static void assertCourseMatches(AcademicGroup group, SubjectInDirection sid) {
        if (!Objects.equals(group.getCourse(), sid.getCourse())) {
            throw new BadRequestException(String.format(
                    "Студент группы %s не может получать оценку по дисциплине %s",
                    formatCourse(group.getCourse()),
                    formatCourse(sid.getCourse())));
        }
    }

    private static String formatCourse(Integer c) {
        return c == null ? "?" : (c + " курс");
    }

    /**
     * Проверка для защитной фильтрации на чтении (исторически неконсистентные строки в БД).
     */
    public static boolean studentProfileMatchesSubjectDirection(StudentProfile sp, SubjectInDirection sid) {
        if (sp == null || sp.getGroup() == null || sid == null || sid.getDirection() == null) {
            return false;
        }
        return Objects.equals(sp.getGroup().getDirection().getId(), sid.getDirection().getId())
                && Objects.equals(sp.getGroup().getCourse(), sid.getCourse());
    }
}
