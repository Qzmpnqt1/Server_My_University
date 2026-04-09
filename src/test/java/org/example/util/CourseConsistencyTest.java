package org.example.util;

import org.example.exception.BadRequestException;
import org.example.model.AcademicGroup;
import org.example.model.Institute;
import org.example.model.StudentProfile;
import org.example.model.StudyDirection;
import org.example.model.SubjectInDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseConsistencyTest {

    private StudyDirection direction(long id) {
        Institute inst = Institute.builder().id(1L).build();
        return StudyDirection.builder().id(id).institute(inst).build();
    }

    private AcademicGroup group(long dirId, int course) {
        return AcademicGroup.builder()
                .id(10L)
                .course(course)
                .direction(direction(dirId))
                .build();
    }

    private SubjectInDirection sid(long dirId, int course) {
        return SubjectInDirection.builder()
                .id(20L)
                .course(course)
                .direction(direction(dirId))
                .build();
    }

    @Test
    @DisplayName("studentProfileMatchesSubjectDirection — совпадение направления и курса")
    void matches_whenDirectionAndCourseEqual() {
        StudentProfile sp = StudentProfile.builder()
                .id(1L)
                .group(group(1, 2))
                .build();
        assertTrue(CourseConsistency.studentProfileMatchesSubjectDirection(sp, sid(1, 2)));
    }

    @Test
    @DisplayName("studentProfileMatchesSubjectDirection — другой курс")
    void noMatch_whenCourseDiffers() {
        StudentProfile sp = StudentProfile.builder()
                .id(1L)
                .group(group(1, 1))
                .build();
        assertFalse(CourseConsistency.studentProfileMatchesSubjectDirection(sp, sid(1, 2)));
    }

    @Test
    @DisplayName("assertStudentProfileMatchesSubjectDirection — бросает при несовпадении курса")
    void assertThrows_whenCourseMismatch() {
        StudentProfile sp = StudentProfile.builder()
                .id(1L)
                .group(group(1, 3))
                .build();
        assertThrows(BadRequestException.class,
                () -> CourseConsistency.assertStudentProfileMatchesSubjectDirection(sp, sid(1, 2)));
    }

    @Test
    @DisplayName("assertGroupMatchesSubjectDirection — бросает при другом направлении")
    void assertGroupThrows_whenDirectionMismatch() {
        AcademicGroup g = group(2, 2);
        SubjectInDirection s = sid(1, 2);
        assertThrows(BadRequestException.class,
                () -> CourseConsistency.assertGroupMatchesSubjectDirection(g, s));
    }
}
