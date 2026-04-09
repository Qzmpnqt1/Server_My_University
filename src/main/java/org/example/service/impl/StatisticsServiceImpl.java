package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.*;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.StatisticsService;
import org.example.service.UniversityScopeService;
import org.example.util.CourseConsistency;
import org.example.util.RussianSort;
import org.example.util.StatisticsFinalAssessmentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private final GradeRepository gradeRepository;
    private final PracticeGradeRepository practiceGradeRepository;
    private final SubjectPracticeRepository subjectPracticeRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final StudyDirectionRepository studyDirectionRepository;
    private final InstituteRepository instituteRepository;
    private final UniversityRepository universityRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final ClassroomRepository classroomRepository;

    @Override
    public SubjectStatisticsResponse getSubjectStatistics(Long subjectDirectionId, String viewerEmail, Long groupId) {
        log.debug("getSubjectStatistics subjectDirectionId={} groupId={} viewer={}", subjectDirectionId, groupId,
                viewerEmail);
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));
        ensureSubjectDirectionAccess(sid, viewerEmail);

        List<StudentProfile> required = resolveRequiredStudentsForSubjectDirection(sid, groupId, viewerEmail);
        int totalRequired = required.size();
        String samplingScope = groupId != null ? "GROUP" : "DIRECTION_ALL_GROUPS";

        List<Grade> allGrades = gradeRepository.findBySubjectDirectionId(subjectDirectionId);
        Map<Long, Grade> gradeByStudentId = allGrades.stream()
                .collect(Collectors.toMap(g -> g.getStudent().getId(), g -> g, (a, b) -> a));

        FinalAssessmentType atype = StatisticsFinalAssessmentUtil.effectiveType(sid);

        if (atype == FinalAssessmentType.CREDIT) {
            int passed = 0;
            int gradedStudents = 0;
            for (StudentProfile sp : required) {
                Grade g = gradeByStudentId.get(sp.getUser().getId());
                if (g != null && g.getCreditStatus() != null) {
                    gradedStudents++;
                    if (Boolean.TRUE.equals(g.getCreditStatus())) {
                        passed++;
                    }
                }
            }
            double creditRate = totalRequired > 0 ? 100.0 * passed / totalRequired : 0.0;
            return SubjectStatisticsResponse.builder()
                    .subjectDirectionId(subjectDirectionId)
                    .directionId(sid.getDirection().getId())
                    .groupIdFilter(groupId)
                    .samplingScope(samplingScope)
                    .averagePerformanceScope(null)
                    .subjectName(sid.getSubject().getName())
                    .assessmentType("CREDIT")
                    .averageGrade(0)
                    .medianGrade(0)
                    .creditRate(round(creditRate))
                    .totalStudents(totalRequired)
                    .gradedStudents(gradedStudents)
                    .missingValues(Math.max(0, totalRequired - gradedStudents))
                    .gradeDistribution(Collections.emptyMap())
                    .build();
        }

        List<Integer> numericGrades = new ArrayList<>();
        for (StudentProfile sp : required) {
            Grade g = gradeByStudentId.get(sp.getUser().getId());
            if (g != null && g.getGrade() != null && g.getGrade() >= 2 && g.getGrade() <= 5) {
                numericGrades.add(g.getGrade());
            }
        }
        int gradedStudents = numericGrades.size();
        return SubjectStatisticsResponse.builder()
                .subjectDirectionId(subjectDirectionId)
                .directionId(sid.getDirection().getId())
                .groupIdFilter(groupId)
                .samplingScope(samplingScope)
                .averagePerformanceScope("EXAM_FINAL_GRADE_2_TO_5")
                .subjectName(sid.getSubject().getName())
                .assessmentType("EXAM")
                .averageGrade(round(average(numericGrades)))
                .medianGrade(round(calculateMedian(numericGrades)))
                .creditRate(0)
                .totalStudents(totalRequired)
                .gradedStudents(gradedStudents)
                .missingValues(Math.max(0, totalRequired - gradedStudents))
                .gradeDistribution(numericGrades.stream()
                        .collect(Collectors.groupingBy(g -> g, Collectors.counting())))
                .build();
    }

    @Override
    public PracticeStatisticsResponse getPracticeStatistics(Long subjectDirectionId, String viewerEmail, Long groupId) {
        log.debug("getPracticeStatistics subjectDirectionId={} groupId={} viewer={}", subjectDirectionId, groupId,
                viewerEmail);
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));
        ensureSubjectDirectionAccess(sid, viewerEmail);

        List<StudentProfile> required = resolveRequiredStudentsForSubjectDirection(sid, groupId, viewerEmail);
        int totalRequired = required.size();
        String samplingScope = groupId != null ? "GROUP" : "DIRECTION_ALL_GROUPS";

        List<SubjectPractice> practices = new ArrayList<>(
                subjectPracticeRepository.findBySubjectDirectionId(subjectDirectionId));
        RussianSort.sortSubjectPractices(practices);

        List<PracticeStatisticsResponse.PracticeDetail> details = new ArrayList<>();
        int totalWith = 0;
        int totalSlots = 0;
        List<Double> perPracticeRawAverages = new ArrayList<>();
        List<Double> perPracticeNormPercents = new ArrayList<>();

        for (SubjectPractice p : practices) {
            List<PracticeGrade> pgs = practiceGradeRepository.findByPracticeId(p.getId());
            Map<Long, PracticeGrade> byStudent = pgs.stream()
                    .collect(Collectors.toMap(pg -> pg.getStudent().getId(), pg -> pg, (a, b) -> a));

            boolean creditPractice = Boolean.TRUE.equals(p.getIsCredit());
            int withResult = 0;
            int passedCredit = 0;
            List<Integer> nums = new ArrayList<>();
            List<Double> normPercentsThisPractice = new ArrayList<>();

            for (StudentProfile sp : required) {
                Long uid = sp.getUser().getId();
                PracticeGrade pg = byStudent.get(uid);
                if (creditPractice) {
                    if (pg != null && pg.getCreditStatus() != null) {
                        withResult++;
                        if (Boolean.TRUE.equals(pg.getCreditStatus())) {
                            passedCredit++;
                        }
                    }
                } else {
                    if (StatisticsFinalAssessmentUtil.practiceResultComplete(pg, p)) {
                        withResult++;
                    }
                    if (pg != null && StatisticsFinalAssessmentUtil.isValidNumericPracticeGrade(pg.getGrade(),
                            p.getMaxGrade())) {
                        int gr = pg.getGrade();
                        nums.add(gr);
                        double np = StatisticsFinalAssessmentUtil.normPercentOfPracticeGrade(gr, p.getMaxGrade());
                        if (!Double.isNaN(np)) {
                            normPercentsThisPractice.add(np);
                        }
                    }
                }
            }

            double completionRate = totalRequired > 0 ? 100.0 * withResult / totalRequired : 0.0;
            Double creditRateVal = creditPractice && totalRequired > 0
                    ? round(100.0 * passedCredit / totalRequired)
                    : null;

            double avgGrade = 0.0;
            Double normalizedAvg = null;
            Double avgNormPercent = null;
            if (!creditPractice) {
                if (!nums.isEmpty()) {
                    double rawAvg = average(nums);
                    avgGrade = round(rawAvg);
                    Double toFive = StatisticsFinalAssessmentUtil.normalizedAverageToFivePoint(rawAvg, p.getMaxGrade());
                    normalizedAvg = toFive != null ? round(toFive) : null;
                    avgNormPercent = round(normPercentsThisPractice.stream()
                            .mapToDouble(Double::doubleValue).average().orElse(0.0));
                    perPracticeRawAverages.add(rawAvg);
                    perPracticeNormPercents.add(avgNormPercent);
                }
            }

            details.add(PracticeStatisticsResponse.PracticeDetail.builder()
                    .practiceId(p.getId())
                    .practiceNumber(p.getPracticeNumber())
                    .practiceTitle(p.getPracticeTitle())
                    .totalRecords(pgs.size())
                    .totalRequiredStudents(totalRequired)
                    .withResult(withResult)
                    .completionRate(round(completionRate))
                    .averageGrade(avgGrade)
                    .creditRate(creditRateVal)
                    .normalizedAverage(normalizedAvg)
                    .averageNormalizedPercent(avgNormPercent)
                    .build());

            totalWith += withResult;
            totalSlots += totalRequired;
        }

        double progress = totalSlots > 0 ? (double) totalWith / totalSlots * 100.0 : 0.0;
        double totalScoreAvg = perPracticeRawAverages.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        Double aggNorm = perPracticeNormPercents.isEmpty()
                ? null
                : round(perPracticeNormPercents.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        return PracticeStatisticsResponse.builder()
                .subjectDirectionId(subjectDirectionId)
                .directionId(sid.getDirection().getId())
                .groupIdFilter(groupId)
                .samplingScope(samplingScope)
                .totalRequiredStudents(totalRequired)
                .subjectName(sid.getSubject().getName())
                .overallProgress(round(progress))
                .totalScoreAverage(round(totalScoreAvg))
                .averageNormalizedPercentAcrossNumericPractices(aggNorm)
                .completionPercentage(round(progress))
                .totalPractices(practices.size())
                .countedValues(totalWith)
                .missingValues(Math.max(0, totalSlots - totalWith))
                .practices(details)
                .build();
    }

    @Override
    public GroupStatisticsResponse getGroupStatistics(Long groupId, String viewerEmail) {
        log.debug("getGroupStatistics groupId={} viewer={}", groupId, viewerEmail);
        AcademicGroup group = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
        ensureGroupAccess(group, viewerEmail);

        List<StudentProfile> profiles = studentProfileRepository.findByGroupId(groupId);
        Integer groupCourse = group.getCourse();
        List<SubjectInDirection> sids = subjectInDirectionRepository.findByDirectionId(group.getDirection().getId())
                .stream()
                .filter(s -> Objects.equals(s.getCourse(), groupCourse))
                .collect(Collectors.toList());
        List<Long> sidIds = sids.stream().map(SubjectInDirection::getId).collect(Collectors.toList());

        List<Integer> allGrades = new ArrayList<>();
        int withDebt = 0;
        int filledFinalCells = 0;
        long totalFinalCells = (long) profiles.size() * sids.size();
        Map<String, List<Integer>> examGradesBySubject = new LinkedHashMap<>();
        Map<String, List<Boolean>> creditResultsBySubject = new LinkedHashMap<>();
        for (SubjectInDirection s : sids) {
            String key = s.getSubject().getName() + " (сем " + s.getSemester() + ")";
            examGradesBySubject.put(key, new ArrayList<>());
            creditResultsBySubject.put(key, new ArrayList<>());
        }

        for (StudentProfile sp : profiles) {
            List<Grade> studentGrades = sidIds.isEmpty() ? List.of()
                    : gradeRepository.findByStudentIdAndSubjectDirectionIdIn(sp.getUser().getId(), sidIds);
            Map<Long, Grade> gm = studentGrades.stream()
                    .collect(Collectors.toMap(g -> g.getSubjectDirection().getId(), g -> g, (a, b) -> a));

            boolean debt = false;
            for (SubjectInDirection s : sids) {
                Grade g = gm.get(s.getId());
                String key = s.getSubject().getName() + " (сем " + s.getSemester() + ")";
                FinalAssessmentType at = StatisticsFinalAssessmentUtil.effectiveType(s);
                if (StatisticsFinalAssessmentUtil.isFinalComplete(g, at)) {
                    filledFinalCells++;
                }
                if (StatisticsFinalAssessmentUtil.isDebtForSubject(g, at)) {
                    debt = true;
                }
                if (g != null && at == FinalAssessmentType.EXAM
                        && g.getGrade() != null && g.getGrade() >= 2 && g.getGrade() <= 5) {
                    allGrades.add(g.getGrade());
                    examGradesBySubject.get(key).add(g.getGrade());
                }
                if (g != null && at == FinalAssessmentType.CREDIT && g.getCreditStatus() != null) {
                    creditResultsBySubject.get(key).add(g.getCreditStatus());
                }
            }
            if (debt) {
                withDebt++;
            }
        }

        Map<String, Double> avgBySubject = new LinkedHashMap<>();
        examGradesBySubject.forEach((k, v) -> {
            if (!v.isEmpty()) {
                avgBySubject.put(k, round(average(v)));
            }
        });
        Map<String, Double> creditPassBySubject = new LinkedHashMap<>();
        creditResultsBySubject.forEach((k, bools) -> {
            if (!bools.isEmpty()) {
                long ok = bools.stream().filter(Boolean::booleanValue).count();
                creditPassBySubject.put(k, round(100.0 * ok / bools.size()));
            }
        });

        return GroupStatisticsResponse.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .averagePerformanceScope("EXAM_FINAL_NUMERIC_2_TO_5")
                .averagePerformance(round(average(allGrades)))
                .debtRate(round(profiles.isEmpty() ? 0.0 : (double) withDebt / profiles.size() * 100.0))
                .studentCount(profiles.size())
                .studentsWithDebt(withDebt)
                .countedValues(filledFinalCells)
                .missingValues(Math.max(0L, totalFinalCells - filledFinalCells))
                .averageBySubject(avgBySubject)
                .creditPassPercentBySubject(creditPassBySubject)
                .build();
    }

    @Override
    public DirectionStatisticsResponse getDirectionStatistics(Long directionId, String viewerEmail) {
        log.debug("getDirectionStatistics directionId={} viewer={}", directionId, viewerEmail);
        StudyDirection dir = studyDirectionRepository.findById(directionId)
                .orElseThrow(() -> new ResourceNotFoundException("Направление не найдено"));
        ensureDirectionAccess(dir, viewerEmail);

        List<AcademicGroup> groups = academicGroupRepository.findByDirectionId(directionId);
        List<DirectionStatisticsResponse.GroupSummary> summaries = new ArrayList<>();
        List<Integer> allGrades = new ArrayList<>();
        int totalStudents = 0, totalDebt = 0;

        for (AcademicGroup g : groups) {
            GroupStatisticsResponse gs = getGroupStatistics(g.getId(), viewerEmail);
            summaries.add(DirectionStatisticsResponse.GroupSummary.builder()
                    .groupId(g.getId()).groupName(g.getName())
                    .averagePerformance(gs.getAveragePerformance())
                    .debtRate(gs.getDebtRate()).studentCount(gs.getStudentCount()).build());
            totalStudents += gs.getStudentCount();
            totalDebt += gs.getStudentsWithDebt();
            collectGradesForGroup(g.getId(), allGrades);
        }

        summaries.sort(Comparator.comparing(DirectionStatisticsResponse.GroupSummary::getGroupName, RussianSort::compareText)
                .thenComparing(DirectionStatisticsResponse.GroupSummary::getGroupId, Comparator.nullsLast(Long::compareTo)));

        return DirectionStatisticsResponse.builder()
                .directionId(directionId)
                .directionName(dir.getName())
                .averagePerformanceScope("EXAM_FINAL_NUMERIC_2_TO_5")
                .averagePerformance(round(average(allGrades)))
                .debtRate(round(totalStudents > 0 ? (double) totalDebt / totalStudents * 100.0 : 0.0))
                .totalStudents(totalStudents)
                .studentsWithDebt(totalDebt)
                .groupCount(groups.size())
                .groups(summaries)
                .build();
    }

    @Override
    public InstituteStatisticsResponse getInstituteStatistics(Long instituteId, String viewerEmail) {
        log.debug("getInstituteStatistics instituteId={} viewer={}", instituteId, viewerEmail);
        ensureAdminInstitute(instituteId, viewerEmail);
        Institute inst = instituteRepository.findById(instituteId)
                .orElseThrow(() -> new ResourceNotFoundException("Институт не найден"));

        List<StudyDirection> directions = studyDirectionRepository.findByInstituteId(instituteId);
        List<InstituteStatisticsResponse.DirectionSummary> summaries = new ArrayList<>();
        List<Integer> allGrades = new ArrayList<>();
        int totalStudents = 0, totalDebt = 0;

        for (StudyDirection d : directions) {
            DirectionStatisticsResponse ds = getDirectionStatistics(d.getId(), viewerEmail);
            summaries.add(InstituteStatisticsResponse.DirectionSummary.builder()
                    .directionId(d.getId()).directionName(d.getName())
                    .averagePerformance(ds.getAveragePerformance())
                    .studentCount(ds.getTotalStudents()).build());
            totalStudents += ds.getTotalStudents();
            totalDebt += ds.getStudentsWithDebt();
            for (AcademicGroup g : academicGroupRepository.findByDirectionId(d.getId())) {
                collectGradesForGroup(g.getId(), allGrades);
            }
        }

        summaries.sort(Comparator.comparing(InstituteStatisticsResponse.DirectionSummary::getDirectionName, RussianSort::compareText)
                .thenComparing(InstituteStatisticsResponse.DirectionSummary::getDirectionId, Comparator.nullsLast(Long::compareTo)));

        return InstituteStatisticsResponse.builder()
                .instituteId(instituteId)
                .instituteName(inst.getName())
                .averagePerformanceScope("EXAM_FINAL_NUMERIC_2_TO_5")
                .averagePerformance(round(average(allGrades)))
                .debtRate(round(totalStudents > 0 ? (double) totalDebt / totalStudents * 100.0 : 0.0))
                .totalStudents(totalStudents)
                .studentsWithDebt(totalDebt)
                .directionCount(directions.size())
                .directions(summaries)
                .build();
    }

    @Override
    public UniversityStatisticsResponse getUniversityStatistics(Long universityId, String viewerEmail) {
        log.debug("getUniversityStatistics universityId={} viewer={}", universityId, viewerEmail);
        ensureAdminUniversity(universityId, viewerEmail);
        University uni = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Университет не найден"));

        List<Institute> institutes = instituteRepository.findByUniversityId(universityId);
        List<UniversityStatisticsResponse.InstituteSummary> summaries = new ArrayList<>();
        List<Integer> allGrades = new ArrayList<>();
        int totalStudents = 0, totalDebt = 0;

        for (Institute inst : institutes) {
            InstituteStatisticsResponse is = getInstituteStatistics(inst.getId(), viewerEmail);
            summaries.add(UniversityStatisticsResponse.InstituteSummary.builder()
                    .instituteId(inst.getId()).instituteName(inst.getName())
                    .averagePerformance(is.getAveragePerformance())
                    .studentCount(is.getTotalStudents()).build());
            totalStudents += is.getTotalStudents();
            totalDebt += is.getStudentsWithDebt();

            for (StudyDirection d : studyDirectionRepository.findByInstituteId(inst.getId())) {
                for (AcademicGroup g : academicGroupRepository.findByDirectionId(d.getId())) {
                    collectGradesForGroup(g.getId(), allGrades);
                }
            }
        }

        summaries.sort(Comparator.comparing(UniversityStatisticsResponse.InstituteSummary::getInstituteName, RussianSort::compareText)
                .thenComparing(UniversityStatisticsResponse.InstituteSummary::getInstituteId, Comparator.nullsLast(Long::compareTo)));

        return UniversityStatisticsResponse.builder()
                .universityId(universityId)
                .universityName(uni.getName())
                .averagePerformanceScope("EXAM_FINAL_NUMERIC_2_TO_5")
                .averagePerformance(round(average(allGrades)))
                .debtRate(round(totalStudents > 0 ? (double) totalDebt / totalStudents * 100.0 : 0.0))
                .totalStudents(totalStudents)
                .studentsWithDebt(totalDebt)
                .instituteCount(institutes.size())
                .institutes(summaries)
                .build();
    }

    @Override
    public ScheduleStatisticsResponse getTeacherScheduleStatistics(Long teacherId, String viewerEmail,
                                                                   Integer weekNumber) {
        log.debug("getTeacherScheduleStatistics teacherId={} week={} viewer={}", teacherId, weekNumber, viewerEmail);
        Users viewer = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
        if (viewer.getUserType() == UserType.TEACHER && !viewer.getId().equals(teacherId)) {
            throw new AccessDeniedException("Доступна только статистика по собственному расписанию");
        }
        ensureAdminTeacher(teacherId, viewerEmail);
        usersRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        List<Schedule> schedules = weekNumber == null
                ? scheduleRepository.findByTeacherId(teacherId)
                : scheduleRepository.findByTeacherIdAndWeekNumber(teacherId, weekNumber);
        return buildScheduleStats("teacher", teacherId, schedules, weekNumber);
    }

    @Override
    public ScheduleStatisticsResponse getGroupScheduleStatistics(Long groupId, String viewerEmail, Integer weekNumber) {
        log.debug("getGroupScheduleStatistics groupId={} week={} viewer={}", groupId, weekNumber, viewerEmail);
        AcademicGroup group = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
        ensureGroupAccess(group, viewerEmail);
        List<Schedule> schedules = weekNumber == null
                ? scheduleRepository.findByGroupId(groupId)
                : scheduleRepository.findByGroupIdAndWeekNumber(groupId, weekNumber);
        return buildScheduleStats("group", groupId, schedules, weekNumber);
    }

    @Override
    public ScheduleStatisticsResponse getClassroomScheduleStatistics(Long classroomId, String viewerEmail,
                                                                       Integer weekNumber) {
        log.debug("getClassroomScheduleStatistics classroomId={} week={} viewer={}", classroomId, weekNumber,
                viewerEmail);
        Users viewer = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
        if (viewer.getUserType() == UserType.TEACHER) {
            throw new AccessDeniedException("Сводка по аудиториям доступна только администратору");
        }
        ensureAdminClassroom(classroomId, viewerEmail);
        List<Schedule> schedules = weekNumber == null
                ? scheduleRepository.findByClassroomId(classroomId)
                : scheduleRepository.findByClassroomIdAndWeekNumber(classroomId, weekNumber);
        return buildScheduleStats("classroom", classroomId, schedules, weekNumber);
    }

    @Override
    public StudentPerformanceSummaryResponse getMyStudentPerformanceSummary(String email, Integer course,
                                                                            Integer semester) {
        log.debug("getMyStudentPerformanceSummary email={} course={} semester={}", email, course, semester);
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getUserType() != UserType.STUDENT) {
            throw new AccessDeniedException("Сводка доступна только студентам");
        }
        StudentProfile sp = studentProfileRepository.findFetchedByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профиль студента не найден"));
        Long directionId = sp.getGroup().getDirection().getId();
        Integer effectiveCourse = course != null ? course : sp.getGroup().getCourse();

        List<SubjectInDirection> sids = subjectInDirectionRepository.findByDirectionId(directionId).stream()
                .filter(sid -> effectiveCourse == null || Objects.equals(effectiveCourse, sid.getCourse()))
                .filter(sid -> semester == null || Objects.equals(semester, sid.getSemester()))
                .collect(Collectors.toList());

        int planned = sids.size();
        List<Long> sidIds = sids.stream().map(SubjectInDirection::getId).collect(Collectors.toList());

        List<Grade> grades = sidIds.isEmpty() ? List.of()
                : gradeRepository.findByStudentIdAndSubjectDirectionIdIn(user.getId(), sidIds);

        Map<Long, SubjectInDirection> sidById = sids.stream()
                .collect(Collectors.toMap(SubjectInDirection::getId, s -> s, (a, b) -> a));
        int subjectsWithFinal = (int) grades.stream()
                .filter(g -> {
                    SubjectInDirection sd = sidById.get(g.getSubjectDirection().getId());
                    FinalAssessmentType at = sd != null && sd.getFinalAssessmentType() != null
                            ? sd.getFinalAssessmentType()
                            : FinalAssessmentType.EXAM;
                    if (at == FinalAssessmentType.CREDIT) {
                        return g.getCreditStatus() != null;
                    }
                    return g.getGrade() != null && g.getGrade() >= 2 && g.getGrade() <= 5;
                })
                .count();
        int credited = (int) grades.stream()
                .filter(g -> {
                    SubjectInDirection sd = sidById.get(g.getSubjectDirection().getId());
                    if (sd == null) return false;
                    FinalAssessmentType at = sd.getFinalAssessmentType() != null
                            ? sd.getFinalAssessmentType()
                            : FinalAssessmentType.EXAM;
                    return at == FinalAssessmentType.CREDIT && Boolean.TRUE.equals(g.getCreditStatus());
                })
                .count();
        List<Integer> nums = grades.stream()
                .filter(g -> {
                    SubjectInDirection sd = sidById.get(g.getSubjectDirection().getId());
                    FinalAssessmentType at = sd != null && sd.getFinalAssessmentType() != null
                            ? sd.getFinalAssessmentType()
                            : FinalAssessmentType.EXAM;
                    return at == FinalAssessmentType.EXAM;
                })
                .map(Grade::getGrade)
                .filter(Objects::nonNull)
                .filter(g -> g >= 2 && g <= 5)
                .collect(Collectors.toList());
        Double avgNum = nums.isEmpty() ? null : round(average(nums));

        List<SubjectPractice> practices = new ArrayList<>();
        for (Long sid : sidIds) {
            practices.addAll(subjectPracticeRepository.findBySubjectDirectionId(sid));
        }
        int totalPractices = practices.size();
        List<Long> pids = practices.stream().map(SubjectPractice::getId).collect(Collectors.toList());
        Map<Long, SubjectPractice> practiceById = practices.stream()
                .collect(Collectors.toMap(SubjectPractice::getId, p -> p, (a, b) -> a));
        Map<Long, PracticeGrade> myPracticeGradesByPracticeId = new HashMap<>();
        if (!pids.isEmpty()) {
            for (PracticeGrade pg : practiceGradeRepository.findByStudentIdAndPracticeIdIn(user.getId(), pids)) {
                myPracticeGradesByPracticeId.put(pg.getPractice().getId(), pg);
            }
        }
        int practicesWithResult = 0;
        for (SubjectPractice pr : practices) {
            PracticeGrade pg = myPracticeGradesByPracticeId.get(pr.getId());
            if (StatisticsFinalAssessmentUtil.practiceResultComplete(pg, pr)) {
                practicesWithResult++;
            }
        }

        List<SubjectPracticeProgressItem> perDiscipline = new ArrayList<>();
        for (SubjectInDirection oneSid : sids) {
            List<SubjectPractice> sprs = subjectPracticeRepository.findBySubjectDirectionId(oneSid.getId());
            int t = sprs.size();
            int done = 0;
            int sumPoints = 0;
            boolean anyNumeric = false;
            for (SubjectPractice pr : sprs) {
                PracticeGrade pg = myPracticeGradesByPracticeId.get(pr.getId());
                if (StatisticsFinalAssessmentUtil.practiceResultComplete(pg, pr)) {
                    done++;
                }
                if (!Boolean.TRUE.equals(pr.getIsCredit()) && pg != null
                        && StatisticsFinalAssessmentUtil.isValidNumericPracticeGrade(pg.getGrade(), pr.getMaxGrade())) {
                    sumPoints += pg.getGrade();
                    anyNumeric = true;
                }
            }
            double pPct = t == 0 ? 0.0 : round(100.0 * done / t);
            perDiscipline.add(SubjectPracticeProgressItem.builder()
                    .subjectDirectionId(oneSid.getId())
                    .subjectName(oneSid.getSubject().getName())
                    .course(oneSid.getCourse())
                    .semester(oneSid.getSemester())
                    .totalPractices(t)
                    .practicesWithResult(done)
                    .practiceProgressPercent(pPct)
                    .sumNumericPracticePoints(anyNumeric ? sumPoints : null)
                    .build());
        }
        perDiscipline.sort(Comparator
                .comparing(SubjectPracticeProgressItem::getSubjectName, RussianSort::compareText)
                .thenComparing(SubjectPracticeProgressItem::getSubjectDirectionId, Comparator.nullsLast(Long::compareTo)));

        double subjPct = planned == 0 ? 0 : round(100.0 * subjectsWithFinal / planned);
        double prPct = totalPractices == 0 ? 0 : round(100.0 * practicesWithResult / totalPractices);

        return StudentPerformanceSummaryResponse.builder()
                .courseFilter(effectiveCourse)
                .semesterFilter(semester)
                .plannedSubjects(planned)
                .subjectsWithFinalResult(subjectsWithFinal)
                .subjectsCredited(credited)
                .averageNumericGrade(avgNum)
                .totalPractices(totalPractices)
                .practicesWithResult(practicesWithResult)
                .subjectCompletionPercent(subjPct)
                .practiceCompletionPercent(prPct)
                .subjectPracticeProgressByDiscipline(perDiscipline)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────

    /**
     * Студенты, для которых дисциплина в направлении обязательна: вся группа или все группы направления.
     */
    private List<StudentProfile> resolveRequiredStudentsForSubjectDirection(SubjectInDirection sid, Long groupId,
                                                                          String viewerEmail) {
        if (groupId != null) {
            AcademicGroup g = academicGroupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
            CourseConsistency.assertGroupMatchesSubjectDirection(g, sid);
            ensureGroupAccess(g, viewerEmail);
            return studentProfileRepository.findByGroupId(groupId);
        }
        Integer sidCourse = sid.getCourse();
        List<AcademicGroup> groups = academicGroupRepository.findByDirectionId(sid.getDirection().getId());
        List<StudentProfile> out = new ArrayList<>();
        for (AcademicGroup g : groups) {
            if (!Objects.equals(g.getCourse(), sidCourse)) {
                continue;
            }
            out.addAll(studentProfileRepository.findByGroupId(g.getId()));
        }
        return out;
    }

    private void collectGradesForGroup(Long groupId, List<Integer> target) {
        AcademicGroup group = academicGroupRepository.findById(groupId).orElse(null);
        if (group == null) return;
        List<StudentProfile> profiles = studentProfileRepository.findByGroupId(groupId);
        Integer gc = group.getCourse();
        List<SubjectInDirection> sids = subjectInDirectionRepository.findByDirectionId(group.getDirection().getId())
                .stream()
                .filter(s -> Objects.equals(s.getCourse(), gc))
                .collect(Collectors.toList());
        List<Long> sidIds = sids.stream().map(SubjectInDirection::getId).collect(Collectors.toList());
        Map<Long, SubjectInDirection> sidEntity = sids.stream()
                .collect(Collectors.toMap(SubjectInDirection::getId, s -> s, (a, b) -> a));

        for (StudentProfile sp : profiles) {
            List<Grade> grades = sidIds.isEmpty() ? List.of()
                    : gradeRepository.findByStudentIdAndSubjectDirectionIdIn(sp.getUser().getId(), sidIds);
            for (Grade g : grades) {
                SubjectInDirection sd = sidEntity.get(g.getSubjectDirection().getId());
                FinalAssessmentType at = sd != null
                        ? StatisticsFinalAssessmentUtil.effectiveType(sd)
                        : FinalAssessmentType.EXAM;
                if (at == FinalAssessmentType.EXAM && g.getGrade() != null && g.getGrade() >= 2 && g.getGrade() <= 5) {
                    target.add(g.getGrade());
                }
            }
        }
    }

    private void ensureSubjectDirectionAccess(SubjectInDirection sid, String viewerEmail) {
        Users u = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
        if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
            Long uni = sid.getDirection().getInstitute().getUniversity().getId();
            universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            universityScopeService.assertSubjectDirectionInUniversity(sid.getId(), uni);
        } else if (u.getUserType() == UserType.TEACHER) {
            TeacherProfile tp = teacherProfileRepository.findByUserId(u.getId())
                    .orElseThrow(() -> new AccessDeniedException("Профиль преподавателя не найден"));
            if (!teacherSubjectRepository.teacherTeachesInDirection(tp.getId(), sid.getDirection().getId())) {
                throw new AccessDeniedException("Нет доступа к статистике по этой дисциплине");
            }
        } else {
            throw new AccessDeniedException("Недостаточно прав");
        }
    }

    private void ensureGroupAccess(AcademicGroup group, String viewerEmail) {
        Users u = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
        if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
            Long uni = group.getDirection().getInstitute().getUniversity().getId();
            universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            universityScopeService.assertAcademicGroupInUniversity(group.getId(), uni);
        } else if (u.getUserType() == UserType.TEACHER) {
            TeacherProfile tp = teacherProfileRepository.findByUserId(u.getId())
                    .orElseThrow(() -> new AccessDeniedException("Профиль преподавателя не найден"));
            if (!teacherSubjectRepository.teacherTeachesInDirection(tp.getId(), group.getDirection().getId())) {
                throw new AccessDeniedException("Нет доступа к статистике этой группы");
            }
        } else {
            throw new AccessDeniedException("Недостаточно прав");
        }
    }

    private void ensureDirectionAccess(StudyDirection dir, String viewerEmail) {
        Users u = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
        if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
            Long uni = dir.getInstitute().getUniversity().getId();
            universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            universityScopeService.assertStudyDirectionInUniversity(dir.getId(), uni);
        } else if (u.getUserType() == UserType.TEACHER) {
            TeacherProfile tp = teacherProfileRepository.findByUserId(u.getId())
                    .orElseThrow(() -> new AccessDeniedException("Профиль преподавателя не найден"));
            if (!teacherSubjectRepository.teacherTeachesInDirection(tp.getId(), dir.getId())) {
                throw new AccessDeniedException("Нет доступа к статистике этого направления");
            }
        } else {
            throw new AccessDeniedException("Недостаточно прав");
        }
    }

    private void ensureAdminInstitute(Long instituteId, String viewerEmail) {
        usersRepository.findByEmail(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                Institute i = instituteRepository.findById(instituteId)
                        .orElseThrow(() -> new ResourceNotFoundException("Институт не найден"));
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, i.getUniversity().getId());
                universityScopeService.assertInstituteInUniversity(instituteId, i.getUniversity().getId());
            }
        });
    }

    private void ensureAdminUniversity(Long universityId, String viewerEmail) {
        usersRepository.findByEmail(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, universityId);
                universityScopeService.assertUniversityMatches(universityId, universityId);
            }
        });
    }

    private void ensureAdminTeacher(Long teacherUserId, String viewerEmail) {
        usersRepository.findByEmail(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                TeacherProfile tp = teacherProfileRepository.findByUserId(teacherUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("Профиль преподавателя не найден"));
                Long uni = Optional.ofNullable(tp.getUniversity())
                        .map(University::getId)
                        .orElseGet(() -> Optional.ofNullable(tp.getInstitute())
                                .map(inst -> inst.getUniversity().getId())
                                .orElseThrow(() -> new AccessDeniedException(
                                        "Не удалось определить вуз преподавателя для проверки доступа")));
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
                universityScopeService.assertUserInUniversity(teacherUserId, uni);
            }
        });
    }

    private void ensureAdminClassroom(Long classroomId, String viewerEmail) {
        usersRepository.findByEmail(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                Classroom c = classroomRepository.findById(classroomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Аудитория не найдена"));
                Long uni = c.getUniversity().getId();
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
                universityScopeService.assertClassroomInUniversity(classroomId, uni);
            }
        });
    }

    private ScheduleStatisticsResponse buildScheduleStats(String scope, Long entityId, List<Schedule> schedules,
                                                            Integer weekNumberFilter) {
        double totalHours = schedules.stream()
                .mapToDouble(s -> {
                    Duration d = Duration.between(s.getStartTime(), s.getEndTime());
                    long minutes = d.isNegative() ? 0L : d.toMinutes();
                    return minutes / 60.0;
                })
                .sum();

        return ScheduleStatisticsResponse.builder()
                .scope(scope)
                .entityId(entityId)
                .weekNumberFilter(weekNumberFilter)
                .totalLessons(schedules.size())
                .totalHours(round(totalHours))
                .byDayOfWeek(schedules.stream()
                        .collect(Collectors.groupingBy(Schedule::getDayOfWeek, TreeMap::new, Collectors.counting())))
                .byWeekNumber(schedules.stream()
                        .collect(Collectors.groupingBy(Schedule::getWeekNumber, TreeMap::new, Collectors.counting())))
                .build();
    }

    private double average(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private double calculateMedian(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        List<Integer> sorted = values.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();
        return size % 2 == 0
                ? (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0
                : sorted.get(size / 2);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
