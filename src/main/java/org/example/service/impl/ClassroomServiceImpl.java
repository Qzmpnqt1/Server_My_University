package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.ClassroomRequest;
import org.example.dto.response.ClassroomResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Classroom;
import org.example.model.University;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.ClassroomRepository;
import org.example.repository.UniversityRepository;
import org.example.repository.UsersRepository;
import org.example.service.ClassroomService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UniversityRepository universityRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<ClassroomResponse> getAll(Long universityId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent()) {
            UserType t = viewer.get().getUserType();
            if (t == UserType.ADMIN || t == UserType.SUPER_ADMIN) {
                var scope = universityScopeService.resolveAdminListScope(viewerEmail, universityId);
                if (scope.allUniversities()) {
                    return classroomRepository.findAll().stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                }
                return classroomRepository.findByUniversityId(scope.universityId()).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
        }
        List<Classroom> classrooms = (universityId != null)
                ? classroomRepository.findByUniversityId(universityId)
                : classroomRepository.findAll();
        return classrooms.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClassroomResponse getById(Long id, String viewerEmail) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + id));
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                Long uni = classroom.getUniversity().getId();
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            }
        });
        return mapToResponse(classroom);
    }

    @Override
    @Transactional
    public ClassroomResponse create(ClassroomRequest request, String adminEmail) {
        universityScopeService.resolveMutationTargetUniversity(adminEmail, request.getUniversityId());
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + request.getUniversityId()));
        Classroom classroom = Classroom.builder()
                .building(request.getBuilding())
                .roomNumber(request.getRoomNumber())
                .capacity(request.getCapacity())
                .university(university)
                .build();
        return mapToResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public ClassroomResponse update(Long id, ClassroomRequest request, String adminEmail) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail, classroom.getUniversity().getId());
        universityScopeService.assertClassroomInUniversity(id, uni);
        universityScopeService.resolveMutationTargetUniversity(adminEmail, request.getUniversityId());
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + request.getUniversityId()));
        classroom.setBuilding(request.getBuilding());
        classroom.setRoomNumber(request.getRoomNumber());
        classroom.setCapacity(request.getCapacity());
        classroom.setUniversity(university);
        return mapToResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail, classroom.getUniversity().getId());
        universityScopeService.assertClassroomInUniversity(id, uni);
        classroomRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private ClassroomResponse mapToResponse(Classroom classroom) {
        return ClassroomResponse.builder()
                .id(classroom.getId())
                .building(classroom.getBuilding())
                .roomNumber(classroom.getRoomNumber())
                .capacity(classroom.getCapacity())
                .universityId(classroom.getUniversity().getId())
                .build();
    }
}
