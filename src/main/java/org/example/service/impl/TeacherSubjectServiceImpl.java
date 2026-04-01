package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.TeacherSubjectRequest;
import org.example.dto.response.TeacherSubjectResponse;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Subject;
import org.example.model.TeacherProfile;
import org.example.model.TeacherSubject;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.SubjectRepository;
import org.example.repository.TeacherProfileRepository;
import org.example.repository.TeacherSubjectRepository;
import org.example.repository.UsersRepository;
import org.example.service.TeacherSubjectService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherSubjectServiceImpl implements TeacherSubjectService {

    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubjectRepository subjectRepository;
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
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));

        if (teacherSubjectRepository.existsByTeacherIdAndSubjectId(request.getTeacherId(), request.getSubjectId())) {
            throw new ConflictException("Teacher already has this subject assigned");
        }

        TeacherSubject entity = TeacherSubject.builder()
                .teacher(teacher)
                .subject(subject)
                .build();
        return mapToResponse(teacherSubjectRepository.save(entity));
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
        return TeacherSubjectResponse.builder()
                .id(entity.getId())
                .teacherId(teacher.getId())
                .teacherName(teacher.getUser().getLastName() + " " + teacher.getUser().getFirstName())
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .build();
    }
}
