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
        if (viewer.isPresent() && viewer.get().getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
            return classroomRepository.findByUniversityId(uni).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
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
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
                universityScopeService.assertClassroomInUniversity(id, uni);
            }
        });
        return mapToResponse(classroom);
    }

    @Override
    @Transactional
    public ClassroomResponse create(ClassroomRequest request, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertUniversityMatches(request.getUniversityId(), uni);
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
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertClassroomInUniversity(id, uni);
        universityScopeService.assertUniversityMatches(request.getUniversityId(), uni);
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + id));
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
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertClassroomInUniversity(id, uni);
        if (!classroomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Classroom not found with id: " + id);
        }
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
