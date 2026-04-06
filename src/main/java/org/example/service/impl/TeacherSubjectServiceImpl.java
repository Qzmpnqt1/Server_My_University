package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.TeacherSubjectReplaceRequest;
import org.example.dto.request.TeacherSubjectRequest;
import org.example.dto.response.TeacherSubjectResponse;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.SubjectInDirectionRepository;
import org.example.repository.TeacherProfileRepository;
import org.example.repository.TeacherSubjectRepository;
import org.example.repository.UsersRepository;
import org.example.service.TeacherSubjectService;
import org.example.service.UniversityScopeService;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherSubjectServiceImpl implements TeacherSubjectService {

    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<TeacherSubjectResponse> getAll(Long teacherId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent()) {
            UserType t = viewer.get().getUserType();
            if (t == UserType.ADMIN) {
                Long uni = universityScopeService.requireCampusUniversityId(viewerEmail);
                if (teacherId != null) {
                    TeacherProfile tp = teacherProfileRepository.findById(teacherId)
                            .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + teacherId));
                    universityScopeService.assertUserInUniversity(tp.getUser().getId(), uni);
                    return teacherSubjectRepository.findByTeacherId(teacherId).stream()
                            .map(this::mapToResponse)
                            .sorted(RussianSort.teacherSubjects())
                            .collect(Collectors.toList());
                }
                return teacherSubjectRepository.findByTeacherInUniversityId(uni).stream()
                        .map(this::mapToResponse)
                        .sorted(RussianSort.teacherSubjects())
                        .collect(Collectors.toList());
            }
            if (t == UserType.SUPER_ADMIN) {
                if (teacherId != null) {
                    teacherProfileRepository.findById(teacherId)
                            .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + teacherId));
                    return teacherSubjectRepository.findByTeacherId(teacherId).stream()
                            .map(this::mapToResponse)
                            .sorted(RussianSort.teacherSubjects())
                            .collect(Collectors.toList());
                }
                return teacherSubjectRepository.findAll().stream()
                        .map(this::mapToResponse)
                        .sorted(RussianSort.teacherSubjects())
                        .collect(Collectors.toList());
            }
        }
        List<TeacherSubject> list = (teacherId != null)
                ? teacherSubjectRepository.findByTeacherId(teacherId)
                : teacherSubjectRepository.findAll();
        return list.stream()
                .map(this::mapToResponse)
                .sorted(RussianSort.teacherSubjects())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherSubjectResponse create(TeacherSubjectRequest request, String adminEmail) {
        TeacherProfile teacher = teacherProfileRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + request.getTeacherId()));
        SubjectInDirection sid = subjectInDirectionRepository.findById(request.getSubjectDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + request.getSubjectDirectionId()));
        Long uni = sid.getDirection().getInstitute().getUniversity().getId();
        universityScopeService.enforceAccessToEntityUniversity(adminEmail, uni);
        universityScopeService.assertUserInUniversity(teacher.getUser().getId(), uni);
        universityScopeService.assertSubjectDirectionInUniversity(request.getSubjectDirectionId(), uni);

        Long sidId = request.getSubjectDirectionId();
        if (teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(request.getTeacherId(), sidId)) {
            throw new ConflictException("Это назначение уже существует для преподавателя");
        }

        TeacherSubject entity = TeacherSubject.builder()
                .teacher(teacher)
                .subjectInDirection(sid)
                .build();
        return mapToResponse(teacherSubjectRepository.save(entity));
    }

    @Override
    @Transactional
    public List<TeacherSubjectResponse> replaceAssignments(Long teacherProfileId,
                                                           TeacherSubjectReplaceRequest request,
                                                           String adminEmail) {
        TeacherProfile teacher = teacherProfileRepository.findById(teacherProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + teacherProfileId));
        Long teacherUni = Optional.ofNullable(teacher.getUniversity())
                .map(University::getId)
                .orElseGet(() -> Optional.ofNullable(teacher.getInstitute())
                        .map(i -> i.getUniversity().getId())
                        .orElse(null));
        if (teacherUni == null) {
            throw new ResourceNotFoundException("Не удалось определить вуз преподавателя для проверки доступа");
        }
        universityScopeService.enforceAccessToEntityUniversity(adminEmail, teacherUni);
        universityScopeService.assertUserInUniversity(teacher.getUser().getId(), teacherUni);

        List<Long> desiredDistinct = request.getSubjectDirectionIds() == null
                ? List.of()
                : request.getSubjectDirectionIds().stream().distinct().toList();

        if (request.getExpectedAssignmentCount() != null) {
            int current = teacherSubjectRepository.findByTeacherId(teacherProfileId).size();
            if (current != request.getExpectedAssignmentCount()) {
                throw new ConflictException(
                        "Список назначений изменился (возможно, другим администратором). Обновите данные и повторите сохранение.");
            }
        }

        for (Long sidId : desiredDistinct) {
            universityScopeService.assertSubjectDirectionInUniversity(sidId, teacherUni);
            if (!subjectInDirectionRepository.existsById(sidId)) {
                throw new ResourceNotFoundException("Позиция учебного плана не найдена: " + sidId);
            }
        }

        List<TeacherSubject> existing = teacherSubjectRepository.findByTeacherId(teacherProfileId);
        Set<Long> desiredSet = new LinkedHashSet<>(desiredDistinct);
        Set<Long> existingDirections = existing.stream()
                .map(ts -> ts.getSubjectInDirection().getId())
                .collect(Collectors.toSet());

        for (TeacherSubject ts : existing) {
            if (!desiredSet.contains(ts.getSubjectInDirection().getId())) {
                teacherSubjectRepository.delete(ts);
            }
        }
        teacherSubjectRepository.flush();

        for (Long sidId : desiredSet) {
            if (!existingDirections.contains(sidId)) {
                SubjectInDirection sidEntity = subjectInDirectionRepository.findById(sidId)
                        .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + sidId));
                TeacherSubject n = TeacherSubject.builder()
                        .teacher(teacher)
                        .subjectInDirection(sidEntity)
                        .build();
                teacherSubjectRepository.save(n);
            }
        }

        return teacherSubjectRepository.findByTeacherId(teacherProfileId).stream()
                .map(this::mapToResponse)
                .sorted(RussianSort.teacherSubjects())
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        TeacherSubject entity = teacherSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherSubject not found with id: " + id));
        Long uni = entity.getSubjectInDirection().getDirection().getInstitute().getUniversity().getId();
        universityScopeService.enforceAccessToEntityUniversity(adminEmail, uni);
        universityScopeService.assertUserInUniversity(entity.getTeacher().getUser().getId(), uni);
        teacherSubjectRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private TeacherSubjectResponse mapToResponse(TeacherSubject entity) {
        TeacherProfile teacher = entity.getTeacher();
        SubjectInDirection sid = entity.getSubjectInDirection();
        StudyDirection dir = sid.getDirection();
        Institute inst = dir.getInstitute();
        Subject subj = sid.getSubject();

        return TeacherSubjectResponse.builder()
                .id(entity.getId())
                .teacherId(teacher.getId())
                .teacherName(teacher.getUser().getLastName() + " " + teacher.getUser().getFirstName())
                .subjectDirectionId(sid.getId())
                .subjectId(subj.getId())
                .subjectName(subj.getName())
                .directionId(dir.getId())
                .directionName(dir.getName())
                .instituteId(inst.getId())
                .instituteName(inst.getName())
                .course(sid.getCourse())
                .semester(sid.getSemester())
                .build();
    }
}
