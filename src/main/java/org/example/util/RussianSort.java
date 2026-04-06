package org.example.util;

import org.example.dto.response.*;
import org.example.model.Classroom;
import org.example.model.Grade;
import org.example.model.SubjectInDirection;
import org.example.model.SubjectPractice;
import org.example.model.Users;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Единая locale-aware сортировка для русскоязычного UI (кириллица/латиница, регистр).
 */
public final class RussianSort {

    private static final Collator COLLATOR = Collator.getInstance(Locale.forLanguageTag("ru-RU"));

    static {
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    private RussianSort() {}

    public static int compareText(String a, String b) {
        return COLLATOR.compare(a == null ? "" : a, b == null ? "" : b);
    }

    public static <T> Comparator<T> byText(java.util.function.Function<T, String> extractor) {
        return (x, y) -> compareText(extractor.apply(x), extractor.apply(y));
    }

    public static int compareUsersByName(Users a, Users b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        int c = compareText(a.getLastName(), b.getLastName());
        if (c != 0) {
            return c;
        }
        c = compareText(a.getFirstName(), b.getFirstName());
        if (c != 0) {
            return c;
        }
        c = compareText(a.getMiddleName(), b.getMiddleName());
        if (c != 0) {
            return c;
        }
        return Long.compare(
                a.getId() == null ? Long.MIN_VALUE : a.getId(),
                b.getId() == null ? Long.MIN_VALUE : b.getId());
    }

    public static Comparator<Classroom> classroomEntityComparator() {
        return Comparator.comparing(Classroom::getBuilding, (x, y) -> compareText(x, y))
                .thenComparing(c -> Objects.toString(c.getRoomNumber(), ""), RussianSort::compareText)
                .thenComparing(Classroom::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static void sortSubjectInDirectionEntities(List<SubjectInDirection> list) {
        Objects.requireNonNull(list);
        list.sort(Comparator
                .comparing((SubjectInDirection e) -> e.getSubject().getName(), RussianSort::compareText)
                .thenComparing(SubjectInDirection::getCourse, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SubjectInDirection::getSemester, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(e -> e.getDirection().getName(), RussianSort::compareText)
                .thenComparing(SubjectInDirection::getId, Comparator.nullsLast(Long::compareTo)));
    }

    /** Учебный порядок практик: номер, затем название, затем id. */
    public static void sortSubjectPractices(List<SubjectPractice> list) {
        Objects.requireNonNull(list);
        list.sort(Comparator
                .comparing(SubjectPractice::getPracticeNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SubjectPractice::getPracticeTitle, RussianSort::compareText)
                .thenComparing(SubjectPractice::getId, Comparator.nullsLast(Long::compareTo)));
    }

    public static Comparator<Grade> gradeEntityBySubjectPlanComparator() {
        return Comparator
                .comparing((Grade g) -> g.getSubjectDirection().getSubject().getName(), RussianSort::compareText)
                .thenComparing(g -> g.getSubjectDirection().getCourse(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(g -> g.getSubjectDirection().getSemester(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Grade::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static Comparator<Grade> gradeEntityByStudentNameComparator() {
        return Comparator
                .comparing((Grade g) -> formatPersonName(g.getStudent()), RussianSort::compareText)
                .thenComparing(Grade::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static String formatPersonName(Users u) {
        if (u == null) {
            return "";
        }
        String n = Objects.toString(u.getLastName(), "").trim() + " " + Objects.toString(u.getFirstName(), "").trim();
        if (u.getMiddleName() != null && !u.getMiddleName().isBlank()) {
            n += " " + u.getMiddleName().trim();
        }
        return n.trim();
    }

    public static String userProfileSortKey(UserProfileResponse u) {
        String name = (Objects.toString(u.getLastName(), "").trim() + " "
                + Objects.toString(u.getFirstName(), "").trim() + " "
                + Objects.toString(u.getMiddleName(), "")).trim();
        if (!name.isBlank()) {
            return name;
        }
        return Objects.toString(u.getEmail(), "");
    }

    public static Comparator<UserProfileResponse> userProfiles() {
        return byText(RussianSort::userProfileSortKey)
                .thenComparing(UserProfileResponse::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static Comparator<TeacherGradingPickResponse> teacherGradingPicks() {
        return byText(TeacherGradingPickResponse::getName)
                .thenComparing(TeacherGradingPickResponse::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static Comparator<TeacherSubjectResponse> teacherSubjects() {
        return byText(TeacherSubjectResponse::getSubjectName)
                .thenComparing(TeacherSubjectResponse::getDirectionName, RussianSort::compareText)
                .thenComparing(TeacherSubjectResponse::getTeacherName, RussianSort::compareText)
                .thenComparing(TeacherSubjectResponse::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static Comparator<SubjectInDirectionResponse> subjectInDirections() {
        return byText(SubjectInDirectionResponse::getSubjectName)
                .thenComparing(SubjectInDirectionResponse::getCourse, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SubjectInDirectionResponse::getSemester, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SubjectInDirectionResponse::getDirectionName, RussianSort::compareText)
                .thenComparing(SubjectInDirectionResponse::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static Comparator<SubjectPracticeResponse> subjectPracticesByNumber() {
        return Comparator.comparing(SubjectPracticeResponse::getPracticeNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SubjectPracticeResponse::getPracticeTitle, RussianSort::compareText)
                .thenComparing(SubjectPracticeResponse::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static Comparator<ClassroomResponse> classroomResponses() {
        return Comparator.comparing(ClassroomResponse::getBuilding, RussianSort::compareText)
                .thenComparing(c -> Objects.toString(c.getRoomNumber(), ""), RussianSort::compareText)
                .thenComparing(ClassroomResponse::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static Comparator<SubjectLessonTypeResponse> subjectLessonTypes() {
        return Comparator
                .comparing(
                        (SubjectLessonTypeResponse s) ->
                                s.getLessonType() == null ? "" : s.getLessonType().name(),
                        RussianSort::compareText)
                .thenComparing(SubjectLessonTypeResponse::getId, Comparator.nullsLast(Long::compareTo));
    }
}
