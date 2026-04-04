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
        if (viewer.isPresent() && viewer.get().getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
            if (teacherId != null) {
                TeacherProfile tp = teacherProfileRepository.findById(teacherId)
                        .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + teacherId));
                universityScopeService.assertUserInUniversity(tp.getUser().getId(), uni);
                return teacherSubjectRepository.findByTeacherId(teacherId).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
            return teacherSubjectRepository.findByTeacherInstituteUniversityId(uni).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        List<TeacherSubject> list = (teacherId != null)
                ? teacherSubjectRepository.findByTeacherId(teacherId)
                : teacherSubjectRepository.findAll();
        return list.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherSubjectResponse create(TeacherSubjectRequest request, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        TeacherProfile teacher = teacherProfileRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + request.getTeacherId()));
        universityScopeService.assertUserInUniversity(teacher.getUser().getId(), uni);

        Long sidId = request.getSubjectDirectionId();
        universityScopeService.assertSubjectDirectionInUniversity(sidId, uni);
        SubjectInDirection sid = subjectInDirectionRepository.findById(sidId)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + sidId));

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
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        TeacherProfile teacher = teacherProfileRepository.findById(teacherProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found with id: " + teacherProfileId));
        universityScopeService.assertUserInUniversity(teacher.getUser().getId(), uni);

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
            universityScopeService.assertSubjectDirectionInUniversity(sidId, uni);
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
                SubjectInDirection sid = subjectInDirectionRepository.findById(sidId)
                        .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + sidId));
                TeacherSubject n = TeacherSubject.builder()
                        .teacher(teacher)
                        .subjectInDirection(sid)
                        .build();
                teacherSubjectRepository.save(n);
            }
        }

        return teacherSubjectRepository.findByTeacherId(teacherProfileId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        TeacherSubject entity = teacherSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherSubject not found with id: " + id));
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
