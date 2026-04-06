package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.StudyDirectionRequest;
import org.example.dto.response.StudyDirectionResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Institute;
import org.example.model.StudyDirection;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.InstituteRepository;
import org.example.repository.StudyDirectionRepository;
import org.example.repository.UsersRepository;
import org.example.service.StudyDirectionService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyDirectionServiceImpl implements StudyDirectionService {

    private final StudyDirectionRepository studyDirectionRepository;
    private final InstituteRepository instituteRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<StudyDirectionResponse> getAll(Long instituteId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent()) {
            UserType t = viewer.get().getUserType();
            if (t == UserType.ADMIN) {
                Long uni = universityScopeService.requireCampusUniversityId(viewerEmail);
                if (instituteId != null) {
                    universityScopeService.assertInstituteInUniversity(instituteId, uni);
                    return studyDirectionRepository.findByInstituteId(instituteId).stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                }
                return studyDirectionRepository.findByInstitute_University_Id(uni).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
            if (t == UserType.SUPER_ADMIN) {
                if (instituteId != null) {
                    Institute i = instituteRepository.findById(instituteId)
                            .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + instituteId));
                    universityScopeService.enforceAccessToEntityUniversity(viewerEmail, i.getUniversity().getId());
                    return studyDirectionRepository.findByInstituteId(instituteId).stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                }
                return studyDirectionRepository.findAll().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
        }
        List<StudyDirection> directions = (instituteId != null)
                ? studyDirectionRepository.findByInstituteId(instituteId)
                : studyDirectionRepository.findAll();
        return directions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StudyDirectionResponse getById(Long id, String viewerEmail) {
        StudyDirection direction = studyDirectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + id));
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                Long uni = direction.getInstitute().getUniversity().getId();
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            }
        });
        return mapToResponse(direction);
    }

    @Override
    @Transactional
    public StudyDirectionResponse create(StudyDirectionRequest request, String adminEmail) {
        Institute institute = instituteRepository.findById(request.getInstituteId())
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + request.getInstituteId()));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail, institute.getUniversity().getId());
        universityScopeService.assertInstituteInUniversity(request.getInstituteId(), uni);
        StudyDirection direction = StudyDirection.builder()
                .name(request.getName())
                .code(request.getCode())
                .institute(institute)
                .build();
        return mapToResponse(studyDirectionRepository.save(direction));
    }

    @Override
    @Transactional
    public StudyDirectionResponse update(Long id, StudyDirectionRequest request, String adminEmail) {
        StudyDirection direction = studyDirectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                direction.getInstitute().getUniversity().getId());
        universityScopeService.assertStudyDirectionInUniversity(id, uni);
        Institute newInst = instituteRepository.findById(request.getInstituteId())
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + request.getInstituteId()));
        universityScopeService.enforceAccessToEntityUniversity(adminEmail, newInst.getUniversity().getId());
        universityScopeService.assertInstituteInUniversity(request.getInstituteId(), newInst.getUniversity().getId());
        direction.setName(request.getName());
        direction.setCode(request.getCode());
        direction.setInstitute(newInst);
        return mapToResponse(studyDirectionRepository.save(direction));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        StudyDirection direction = studyDirectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                direction.getInstitute().getUniversity().getId());
        universityScopeService.assertStudyDirectionInUniversity(id, uni);
        studyDirectionRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private StudyDirectionResponse mapToResponse(StudyDirection direction) {
        return StudyDirectionResponse.builder()
                .id(direction.getId())
                .name(direction.getName())
                .code(direction.getCode())
                .instituteId(direction.getInstitute().getId())
                .instituteName(direction.getInstitute().getName())
                .build();
    }
}
