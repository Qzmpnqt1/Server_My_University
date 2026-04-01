package org.example.service;

import org.example.dto.response.*;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private GradeRepository gradeRepository;
    @Mock private PracticeGradeRepository practiceGradeRepository;
    @Mock private SubjectPracticeRepository subjectPracticeRepository;
    @Mock private SubjectInDirectionRepository subjectInDirectionRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private AcademicGroupRepository academicGroupRepository;
    @Mock private StudyDirectionRepository studyDirectionRepository;
    @Mock private InstituteRepository instituteRepository;
    @Mock private UniversityRepository universityRepository;
    @Mock private UsersRepository usersRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private Subject subject;
    private SubjectInDirection sid;
    private Users s1, s2, s3, s4;
    private Institute institute;
    private University university;
    private StudyDirection direction;
    private AcademicGroup group;

    @BeforeEach
    void setUp() {
        university = University.builder().id(1L).name("МГУ").build();
        institute = Institute.builder().id(1L).name("ИТИ").university(university).build();
        direction = StudyDirection.builder().id(1L).name("Информатика").institute(institute).build();
        group = AcademicGroup.builder().id(1L).name("ИТ-201").course(2).direction(direction).build();

        subject = Subject.builder().id(1L).name("Математика").build();
        sid = SubjectInDirection.builder().id(1L).subject(subject).semester(1).course(1).direction(direction).build();

        s1 = Users.builder().id(10L).email("s1@t.ru").firstName("А").lastName("Б").userType(UserType.STUDENT).build();
        s2 = Users.builder().id(11L).email("s2@t.ru").firstName("В").lastName("Г").userType(UserType.STUDENT).build();
        s3 = Users.builder().id(12L).email("s3@t.ru").firstName("Д").lastName("Е").userType(UserType.STUDENT).build();
        s4 = Users.builder().id(13L).email("s4@t.ru").firstName("Ж").lastName("З").userType(UserType.STUDENT).build();
    }

    // ── Subject statistics ───────────────────────────────────────

    @Test
    @DisplayName("Average calculated from numeric grades, nulls excluded")
    void subjectAverage() {
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(
                grade(s1, 4), grade(s2, null, true), grade(s3, 2)));

        SubjectStatisticsResponse r = statisticsService.getSubjectStatistics(1L, "viewer@test.ru");

        assertEquals(3.0, r.getAverageGrade(), 0.01);
        assertEquals(3, r.getTotalStudents());
        assertEquals(2, r.getGradedStudents());
        assertEquals("Математика", r.getSubjectName());
    }

    @Test
    @DisplayName("Median with even number of grades: (3+4)/2 = 3.5")
    void subjectMedianEven() {
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(
                grade(s1, 2), grade(s2, 5), grade(s3, 3), grade(s4, 4)));

        SubjectStatisticsResponse r = statisticsService.getSubjectStatistics(1L, "viewer@test.ru");
        assertEquals(3.5, r.getMedianGrade(), 0.01);
    }

    @Test
    @DisplayName("Median with odd number of grades: sorted [2,3,5] → 3")
    void subjectMedianOdd() {
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(
                grade(s1, 5), grade(s2, 2), grade(s3, 3)));

        SubjectStatisticsResponse r = statisticsService.getSubjectStatistics(1L, "viewer@test.ru");
        assertEquals(3.0, r.getMedianGrade(), 0.01);
    }

    @Test
    @DisplayName("Credit rate: 2/3 = 66.67%")
    void subjectCreditRate() {
        SubjectInDirection sidCredit = SubjectInDirection.builder()
                .id(1L).subject(subject).semester(1).course(1).direction(direction)
                .finalAssessmentType(FinalAssessmentType.CREDIT)
                .build();
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sidCredit));
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(
                grade(s1, null, true), grade(s2, null, false), grade(s3, null, true)));

        SubjectStatisticsResponse r = statisticsService.getSubjectStatistics(1L, "viewer@test.ru");
        assertEquals(66.67, r.getCreditRate(), 0.01);
    }

    @Test
    @DisplayName("Empty data returns zeros")
    void subjectEmpty() {
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of());

        SubjectStatisticsResponse r = statisticsService.getSubjectStatistics(1L, "viewer@test.ru");
        assertEquals(0.0, r.getAverageGrade());
        assertEquals(0.0, r.getMedianGrade());
        assertEquals(0.0, r.getCreditRate());
        assertEquals(0, r.getTotalStudents());
    }

    @Test
    @DisplayName("Grade distribution counts correctly")
    void subjectDistribution() {
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(
                grade(s1, 5), grade(s2, 5), grade(s3, 3)));

        SubjectStatisticsResponse r = statisticsService.getSubjectStatistics(1L, "viewer@test.ru");
        assertEquals(2L, r.getGradeDistribution().get(5));
        assertEquals(1L, r.getGradeDistribution().get(3));
    }

    @Test
    @DisplayName("Subject not found throws ResourceNotFoundException")
    void subjectNotFound() {
        when(subjectInDirectionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> statisticsService.getSubjectStatistics(99L, "viewer@test.ru"));
    }

    // ── Practice statistics ──────────────────────────────────────

    @Test
    @DisplayName("Practice statistics with completion and normalized average")
    void practiceStats() {
        SubjectPractice p = SubjectPractice.builder()
                .id(1L).subjectDirection(sid).practiceNumber(1)
                .practiceTitle("Лаб 1").maxGrade(10).isCredit(false).build();

        PracticeGrade pg1 = PracticeGrade.builder().id(1L).student(s1).practice(p).grade(4).build();
        PracticeGrade pg2 = PracticeGrade.builder().id(2L).student(s2).practice(p).grade(5).build();

        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(subjectPracticeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(p));
        when(practiceGradeRepository.findByPracticeId(1L)).thenReturn(List.of(pg1, pg2));

        PracticeStatisticsResponse r = statisticsService.getPracticeStatistics(1L, "viewer@test.ru");

        assertEquals(1, r.getTotalPractices());
        assertEquals(100.0, r.getOverallProgress());
        assertEquals(4.5, r.getTotalScoreAverage(), 0.01);

        PracticeStatisticsResponse.PracticeDetail d = r.getPractices().get(0);
        assertEquals(100.0, d.getCompletionRate());
        assertEquals(4.5, d.getAverageGrade(), 0.01);
        assertNull(d.getNormalizedAverage());
        assertNull(d.getCreditRate());
    }

    @Test
    @DisplayName("Practice with credit type includes creditRate")
    void practiceCreditType() {
        SubjectPractice p = SubjectPractice.builder()
                .id(2L).subjectDirection(sid).practiceNumber(2)
                .practiceTitle("Зачёт").isCredit(true).maxGrade(null).build();

        PracticeGrade pg1 = PracticeGrade.builder().id(1L).student(s1).practice(p)
                .grade(null).creditStatus(true).build();
        PracticeGrade pg2 = PracticeGrade.builder().id(2L).student(s2).practice(p)
                .grade(null).creditStatus(false).build();

        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(subjectPracticeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(p));
        when(practiceGradeRepository.findByPracticeId(2L)).thenReturn(List.of(pg1, pg2));

        PracticeStatisticsResponse r = statisticsService.getPracticeStatistics(1L, "viewer@test.ru");

        PracticeStatisticsResponse.PracticeDetail d = r.getPractices().get(0);
        assertNotNull(d.getCreditRate());
        assertEquals(50.0, d.getCreditRate());
        assertNull(d.getNormalizedAverage());
    }

    @Test
    @DisplayName("Empty practices return zeros")
    void practiceEmpty() {
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(sid));
        when(subjectPracticeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of());

        PracticeStatisticsResponse r = statisticsService.getPracticeStatistics(1L, "viewer@test.ru");
        assertEquals(0, r.getTotalPractices());
        assertEquals(0.0, r.getOverallProgress());
    }

    // ── Group statistics ─────────────────────────────────────────

    @Test
    @DisplayName("Group statistics with debt calculation")
    void groupStats() {
        StudentProfile sp1 = StudentProfile.builder().id(1L).user(s1).group(group).build();
        StudentProfile sp2 = StudentProfile.builder().id(2L).user(s2).group(group).build();

        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentProfileRepository.findByGroupId(1L)).thenReturn(List.of(sp1, sp2));
        when(subjectInDirectionRepository.findByDirectionId(1L)).thenReturn(List.of(sid));
        when(gradeRepository.findByStudentIdAndSubjectDirectionIdIn(10L, List.of(1L)))
                .thenReturn(List.of(grade(s1, 5)));
        when(gradeRepository.findByStudentIdAndSubjectDirectionIdIn(11L, List.of(1L)))
                .thenReturn(List.of(grade(s2, 2)));

        GroupStatisticsResponse r = statisticsService.getGroupStatistics(1L, "viewer@test.ru");

        assertEquals("ИТ-201", r.getGroupName());
        assertEquals(2, r.getStudentCount());
        assertEquals(3.5, r.getAveragePerformance(), 0.01);
        assertEquals(1, r.getStudentsWithDebt());
        assertEquals(50.0, r.getDebtRate());
    }

    @Test
    @DisplayName("Student missing grade counts as debt")
    void groupMissingGradeIsDebt() {
        StudentProfile sp1 = StudentProfile.builder().id(1L).user(s1).group(group).build();

        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentProfileRepository.findByGroupId(1L)).thenReturn(List.of(sp1));
        when(subjectInDirectionRepository.findByDirectionId(1L)).thenReturn(List.of(sid));
        when(gradeRepository.findByStudentIdAndSubjectDirectionIdIn(10L, List.of(1L)))
                .thenReturn(List.of());

        GroupStatisticsResponse r = statisticsService.getGroupStatistics(1L, "viewer@test.ru");

        assertEquals(1, r.getStudentsWithDebt());
        assertEquals(100.0, r.getDebtRate());
    }

    @Test
    @DisplayName("Group not found throws ResourceNotFoundException")
    void groupNotFound() {
        when(academicGroupRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> statisticsService.getGroupStatistics(99L, "viewer@test.ru"));
    }

    // ── Direction statistics ─────────────────────────────────────

    @Test
    @DisplayName("Direction aggregates multiple groups")
    void directionStats() {
        AcademicGroup g2 = AcademicGroup.builder().id(2L).name("ИТ-202").course(2).direction(direction).build();
        StudentProfile sp1 = StudentProfile.builder().id(1L).user(s1).group(group).build();
        StudentProfile sp2 = StudentProfile.builder().id(2L).user(s2).group(g2).build();

        when(studyDirectionRepository.findById(1L)).thenReturn(Optional.of(direction));
        when(academicGroupRepository.findByDirectionId(1L)).thenReturn(List.of(group, g2));
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(academicGroupRepository.findById(2L)).thenReturn(Optional.of(g2));
        when(studentProfileRepository.findByGroupId(1L)).thenReturn(List.of(sp1));
        when(studentProfileRepository.findByGroupId(2L)).thenReturn(List.of(sp2));
        when(subjectInDirectionRepository.findByDirectionId(1L)).thenReturn(List.of(sid));
        when(gradeRepository.findByStudentIdAndSubjectDirectionIdIn(10L, List.of(1L)))
                .thenReturn(List.of(grade(s1, 5)));
        when(gradeRepository.findByStudentIdAndSubjectDirectionIdIn(11L, List.of(1L)))
                .thenReturn(List.of(grade(s2, 3)));

        DirectionStatisticsResponse r = statisticsService.getDirectionStatistics(1L, "viewer@test.ru");

        assertEquals("Информатика", r.getDirectionName());
        assertEquals(2, r.getTotalStudents());
        assertEquals(2, r.getGroupCount());
        assertEquals(4.0, r.getAveragePerformance(), 0.01);
        assertEquals(2, r.getGroups().size());
    }

    // ── Institute statistics ─────────────────────────────────────

    @Test
    @DisplayName("Institute aggregates directions")
    void instituteStats() {
        StudentProfile sp1 = StudentProfile.builder().id(1L).user(s1).group(group).build();

        when(instituteRepository.findById(1L)).thenReturn(Optional.of(institute));
        when(studyDirectionRepository.findByInstituteId(1L)).thenReturn(List.of(direction));
        when(studyDirectionRepository.findById(1L)).thenReturn(Optional.of(direction));
        when(academicGroupRepository.findByDirectionId(1L)).thenReturn(List.of(group));
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentProfileRepository.findByGroupId(1L)).thenReturn(List.of(sp1));
        when(subjectInDirectionRepository.findByDirectionId(1L)).thenReturn(List.of(sid));
        when(gradeRepository.findByStudentIdAndSubjectDirectionIdIn(10L, List.of(1L)))
                .thenReturn(List.of(grade(s1, 4)));

        InstituteStatisticsResponse r = statisticsService.getInstituteStatistics(1L, "viewer@test.ru");

        assertEquals("ИТИ", r.getInstituteName());
        assertEquals(1, r.getTotalStudents());
        assertEquals(4.0, r.getAveragePerformance());
        assertEquals(1, r.getDirectionCount());
    }

    // ── University statistics ────────────────────────────────────

    @Test
    @DisplayName("University aggregates institutes")
    void universityStats() {
        StudentProfile sp1 = StudentProfile.builder().id(1L).user(s1).group(group).build();

        when(universityRepository.findById(1L)).thenReturn(Optional.of(university));
        when(instituteRepository.findByUniversityId(1L)).thenReturn(List.of(institute));
        when(instituteRepository.findById(1L)).thenReturn(Optional.of(institute));
        when(studyDirectionRepository.findByInstituteId(1L)).thenReturn(List.of(direction));
        when(studyDirectionRepository.findById(1L)).thenReturn(Optional.of(direction));
        when(academicGroupRepository.findByDirectionId(1L)).thenReturn(List.of(group));
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentProfileRepository.findByGroupId(1L)).thenReturn(List.of(sp1));
        when(subjectInDirectionRepository.findByDirectionId(1L)).thenReturn(List.of(sid));
        when(gradeRepository.findByStudentIdAndSubjectDirectionIdIn(10L, List.of(1L)))
                .thenReturn(List.of(grade(s1, 5)));

        UniversityStatisticsResponse r = statisticsService.getUniversityStatistics(1L, "viewer@test.ru");

        assertEquals("МГУ", r.getUniversityName());
        assertEquals(1, r.getTotalStudents());
        assertEquals(5.0, r.getAveragePerformance());
        assertEquals(1, r.getInstituteCount());
    }

    // ── Schedule statistics ──────────────────────────────────────

    @Test
    @DisplayName("Teacher schedule statistics")
    void teacherScheduleStats() {
        Users teacher = Users.builder().id(1L).userType(UserType.TEACHER).build();
        Schedule sc1 = Schedule.builder().id(1L).dayOfWeek(1).weekNumber(1)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30)).build();
        Schedule sc2 = Schedule.builder().id(2L).dayOfWeek(1).weekNumber(2)
                .startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(12, 30)).build();

        when(usersRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(scheduleRepository.findByTeacherId(1L)).thenReturn(List.of(sc1, sc2));

        ScheduleStatisticsResponse r = statisticsService.getTeacherScheduleStatistics(1L, "viewer@test.ru");

        assertEquals("teacher", r.getScope());
        assertEquals(2, r.getTotalLessons());
        assertEquals(3.0, r.getTotalHours(), 0.01);
        assertEquals(2L, r.getByDayOfWeek().get(1));
        assertEquals(1L, r.getByWeekNumber().get(1));
        assertEquals(1L, r.getByWeekNumber().get(2));
    }

    @Test
    @DisplayName("Group schedule statistics")
    void groupScheduleStats() {
        Schedule sc = Schedule.builder().id(1L).dayOfWeek(3).weekNumber(1)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(15, 30)).build();

        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroupId(1L)).thenReturn(List.of(sc));

        ScheduleStatisticsResponse r = statisticsService.getGroupScheduleStatistics(1L, "viewer@test.ru");

        assertEquals("group", r.getScope());
        assertEquals(1, r.getTotalLessons());
        assertEquals(1.5, r.getTotalHours(), 0.01);
    }

    @Test
    @DisplayName("Classroom schedule with no entries returns 0")
    void classroomScheduleEmpty() {
        when(scheduleRepository.findByClassroomId(99L)).thenReturn(List.of());

        ScheduleStatisticsResponse r = statisticsService.getClassroomScheduleStatistics(99L, "viewer@test.ru");

        assertEquals(0, r.getTotalLessons());
        assertEquals(0.0, r.getTotalHours());
    }

    @Test
    @DisplayName("Teacher not found throws ResourceNotFoundException")
    void teacherScheduleNotFound() {
        when(usersRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> statisticsService.getTeacherScheduleStatistics(99L, "viewer@test.ru"));
    }

    // ── helpers ──────────────────────────────────────────────────

    private Grade grade(Users student, Integer gradeVal) {
        return Grade.builder().student(student).subjectDirection(sid).grade(gradeVal).build();
    }

    private Grade grade(Users student, Integer gradeVal, Boolean credit) {
        return Grade.builder().student(student).subjectDirection(sid).grade(gradeVal).creditStatus(credit).build();
    }
}
