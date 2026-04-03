package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.*;
import org.example.exception.AccessDeniedException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.StatisticsService;
import org.example.service.UniversityScopeService;
import org.example.util.StatisticsFinalAssessmentUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

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

    @Override
    public SubjectStatisticsResponse getSubjectStatistics(Long subjectDirectionId, String viewerEmail) {
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));
        ensureSubjectDirectionAccess(sid, viewerEmail);

        FinalAssessmentType atype = sid.getFinalAssessmentType() != null
                ? sid.getFinalAssessmentType()
                : FinalAssessmentType.EXAM;
        List<Grade> grades = gradeRepository.findBySubjectDirectionId(subjectDirectionId);

        if (atype == FinalAssessmentType.CREDIT) {
            long creditTrue = grades.stream()
                    .filter(g -> Boolean.TRUE.equals(g.getCreditStatus())).count();
            long creditDefined = grades.stream()
                    .filter(g -> g.getCreditStatus() != null).count();
            int gradedStudents = (int) grades.stream()
                    .filter(g -> g.getCreditStatus() != null).count();
            return SubjectStatisticsResponse.builder()
                    .subjectDirectionId(subjectDirectionId)
                    .subjectName(sid.getSubject().getName())
                    .assessmentType("CREDIT")
                    .averageGrade(0)
                    .medianGrade(0)
                    .creditRate(round(creditDefined > 0 ? (double) creditTrue / creditDefined * 100.0 : 0.0))
                    .totalStudents(grades.size())
                    .gradedStudents(gradedStudents)
                    .missingValues(grades.size() - gradedStudents)
                    .gradeDistribution(Collections.emptyMap())
                    .build();
        }

        List<Integer> numericGrades = grades.stream()
                .map(Grade::getGrade)
                .filter(Objects::nonNull)
                .filter(g -> g >= 2 && g <= 5)
                .collect(Collectors.toList());

        int gradedStudents = (int) grades.stream()
                .filter(g -> g.getGrade() != null && g.getGrade() >= 2 && g.getGrade() <= 5)
                .count();

        return SubjectStatisticsResponse.builder()
                .subjectDirectionId(subjectDirectionId)
                .subjectName(sid.getSubject().getName())
                .assessmentType("EXAM")
                .averageGrade(round(average(numericGrades)))
                .medianGrade(round(calculateMedian(numericGrades)))
                .creditRate(0)
                .totalStudents(grades.size())
                .gradedStudents(gradedStudents)
                .missingValues(grades.size() - gradedStudents)
                .gradeDistribution(numericGrades.stream()
                        .collect(Collectors.groupingBy(g -> g, Collectors.counting())))
                .build();
    }

    @Override
    public PracticeStatisticsResponse getPracticeStatistics(Long subjectDirectionId, String viewerEmail) {
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));
        ensureSubjectDirectionAccess(sid, viewerEmail);

        List<SubjectPractice> practices = subjectPracticeRepository.findBySubjectDirectionId(subjectDirectionId);

        List<PracticeStatisticsResponse.PracticeDetail> details = new ArrayList<>();
        int totalWith = 0, totalExpected = 0;
        double scoreSum = 0.0;
        int scoreCount = 0;

        List<Double> practiceReferenceAverages = new ArrayList<>();

        for (SubjectPractice p : practices) {
            List<PracticeGrade> pgs = practiceGradeRepository.findByPracticeId(p.getId());
            int total = pgs.size();
            boolean creditPractice = Boolean.TRUE.equals(p.getIsCredit());
            int withResult;
            List<Integer> nums;
            Double creditRateVal = null;
            Double normalizedAvg = null;
            if (creditPractice) {
                withResult = (int) pgs.stream().filter(pg -> pg.getCreditStatus() != null).count();
                nums = Collections.emptyList();
                long cTrue = pgs.stream().filter(pg -> Boolean.TRUE.equals(pg.getCreditStatus())).count();
                long cDef = pgs.stream().filter(pg -> pg.getCreditStatus() != null).count();
                creditRateVal = cDef > 0 ? round((double) cTrue / cDef * 100.0) : null;
            } else {
                withResult = (int) pgs.stream()
                        .filter(pg -> pg.getGrade() != null && pg.getGrade() >= 2 && pg.getGrade() <= 5)
                        .count();
                nums = pgs.stream()
                        .map(PracticeGrade::getGrade)
                        .filter(Objects::nonNull)
                        .filter(g -> g >= 2 && g <= 5)
                        .collect(Collectors.toList());
                if (!nums.isEmpty()) {
                    double rawAvg = average(nums);
                    Double toFive = StatisticsFinalAssessmentUtil.normalizedAverageToFivePoint(rawAvg, p.getMaxGrade());
                    normalizedAvg = toFive != null ? round(toFive) : null;
                    practiceReferenceAverages.add(toFive != null ? toFive : rawAvg);
                }
            }

            details.add(PracticeStatisticsResponse.PracticeDetail.builder()
                    .practiceId(p.getId())
                    .practiceNumber(p.getPracticeNumber())
                    .practiceTitle(p.getPracticeTitle())
                    .totalRecords(total)
                    .withResult(withResult)
                    .completionRate(round(total > 0 ? (double) withResult / total * 100.0 : 0.0))
                    .averageGrade(creditPractice ? 0.0 : round(nums.isEmpty() ? 0.0 : average(nums)))
                    .creditRate(creditRateVal)
                    .normalizedAverage(normalizedAvg)
                    .build());

            totalWith += withResult;
            totalExpected += total;
            for (int g : nums) {
                scoreSum += g;
                scoreCount++;
            }
        }

        double progress = totalExpected > 0 ? (double) totalWith / totalExpected * 100.0 : 0.0;

        double totalScoreAvg = practiceReferenceAverages.isEmpty()
                ? (scoreCount > 0 ? scoreSum / scoreCount : 0.0)
                : practiceReferenceAverages.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return PracticeStatisticsResponse.builder()
                .subjectDirectionId(subjectDirectionId)
                .subjectName(sid.getSubject().getName())
                .overallProgress(round(progress))
                .totalScoreAverage(round(totalScoreAvg))
                .completionPercentage(round(progress))
                .totalPractices(practices.size())
                .countedValues(totalExpected)
                .missingValues(totalExpected - totalWith)
                .practices(details)
                .build();
    }

    @Override
    public GroupStatisticsResponse getGroupStatistics(Long groupId, String viewerEmail) {
        AcademicGroup group = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
        ensureGroupAccess(group, viewerEmail);

        List<StudentProfile> profiles = studentProfileRepository.findByGroupId(groupId);
        List<SubjectInDirection> sids = subjectInDirectionRepository.findByDirectionId(group.getDirection().getId());
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

        return DirectionStatisticsResponse.builder()
                .directionId(directionId)
                .directionName(dir.getName())
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

        return InstituteStatisticsResponse.builder()
                .instituteId(instituteId)
                .instituteName(inst.getName())
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

        return UniversityStatisticsResponse.builder()
                .universityId(universityId)
                .universityName(uni.getName())
                .averagePerformance(round(average(allGrades)))
                .debtRate(round(totalStudents > 0 ? (double) totalDebt / totalStudents * 100.0 : 0.0))
                .totalStudents(totalStudents)
                .studentsWithDebt(totalDebt)
                .instituteCount(institutes.size())
                .institutes(summaries)
                .build();
    }

    @Override
    public ScheduleStatisticsResponse getTeacherScheduleStatistics(Long teacherId, String viewerEmail) {
        Users viewer = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
        if (viewer.getUserType() == UserType.TEACHER && !viewer.getId().equals(teacherId)) {
            throw new AccessDeniedException("Доступна только статистика по собственному расписанию");
        }
        ensureAdminTeacher(teacherId, viewerEmail);
        usersRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        return buildScheduleStats("teacher", teacherId, scheduleRepository.findByTeacherId(teacherId));
    }

    @Override
    public ScheduleStatisticsResponse getGroupScheduleStatistics(Long groupId, String viewerEmail) {
        AcademicGroup group = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
        ensureGroupAccess(group, viewerEmail);
        return buildScheduleStats("group", groupId, scheduleRepository.findByGroupId(groupId));
    }

    @Override
    public ScheduleStatisticsResponse getClassroomScheduleStatistics(Long classroomId, String viewerEmail) {
        Users viewer = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
        if (viewer.getUserType() == UserType.TEACHER) {
            throw new AccessDeniedException("Сводка по аудиториям доступна только администратору");
        }
        ensureAdminClassroom(classroomId, viewerEmail);
        return buildScheduleStats("classroom", classroomId,
                scheduleRepository.findByClassroomId(classroomId));
    }

    @Override
    public StudentPerformanceSummaryResponse getMyStudentPerformanceSummary(String email, Integer course,
                                                                            Integer semester) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getUserType() != UserType.STUDENT) {
            throw new AccessDeniedException("Сводка доступна только студентам");
        }
        StudentProfile sp = studentProfileRepository.findFetchedByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профиль студента не найден"));
        Long directionId = sp.getGroup().getDirection().getId();

        List<SubjectInDirection> sids = subjectInDirectionRepository.findByDirectionId(directionId).stream()
                .filter(sid -> course == null || Objects.equals(course, sid.getCourse()))
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
        int practicesWithResult = 0;
        if (!pids.isEmpty()) {
            practicesWithResult = (int) practiceGradeRepository.findByStudentIdAndPracticeIdIn(user.getId(), pids).stream()
                    .filter(pg -> {
                        SubjectPractice pr = practiceById.get(pg.getPractice().getId());
                        if (pr == null) return false;
                        if (Boolean.TRUE.equals(pr.getIsCredit())) {
                            return pg.getCreditStatus() != null;
                        }
                        return pg.getGrade() != null && pg.getGrade() >= 2 && pg.getGrade() <= 5;
                    })
                    .map(pg -> pg.getPractice().getId())
                    .distinct()
                    .count();
        }

        double subjPct = planned == 0 ? 0 : round(100.0 * subjectsWithFinal / planned);
        double prPct = totalPractices == 0 ? 0 : round(100.0 * practicesWithResult / totalPractices);

        return StudentPerformanceSummaryResponse.builder()
                .courseFilter(course)
                .semesterFilter(semester)
                .plannedSubjects(planned)
                .subjectsWithFinalResult(subjectsWithFinal)
                .subjectsCredited(credited)
                .averageNumericGrade(avgNum)
                .totalPractices(totalPractices)
                .practicesWithResult(practicesWithResult)
                .subjectCompletionPercent(subjPct)
                .practiceCompletionPercent(prPct)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────

    private void collectGradesForGroup(Long groupId, List<Integer> target) {
        AcademicGroup group = academicGroupRepository.findById(groupId).orElse(null);
        if (group == null) return;
        List<StudentProfile> profiles = studentProfileRepository.findByGroupId(groupId);
        List<SubjectInDirection> sids = subjectInDirectionRepository.findByDirectionId(group.getDirection().getId());
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
        if (u.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
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
        if (u.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
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
        if (u.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
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
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
                universityScopeService.assertInstituteInUniversity(instituteId, uni);
            }
        });
    }

    private void ensureAdminUniversity(Long universityId, String viewerEmail) {
        usersRepository.findByEmail(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
                universityScopeService.assertUniversityMatches(universityId, uni);
            }
        });
    }

    private void ensureAdminTeacher(Long teacherUserId, String viewerEmail) {
        usersRepository.findByEmail(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
                universityScopeService.assertUserInUniversity(teacherUserId, uni);
            }
        });
    }

    private void ensureAdminClassroom(Long classroomId, String viewerEmail) {
        usersRepository.findByEmail(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
                universityScopeService.assertClassroomInUniversity(classroomId, uni);
            }
        });
    }

    private ScheduleStatisticsResponse buildScheduleStats(String scope, Long entityId, List<Schedule> schedules) {
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
