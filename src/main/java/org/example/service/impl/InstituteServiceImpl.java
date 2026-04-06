package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.InstituteRequest;
import org.example.dto.response.InstituteResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Institute;
import org.example.model.University;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.InstituteRepository;
import org.example.repository.UniversityRepository;
import org.example.repository.UsersRepository;
import org.example.service.InstituteService;
import org.example.service.UniversityScopeService;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstituteServiceImpl implements InstituteService {

    private final InstituteRepository instituteRepository;
    private final UniversityRepository universityRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<InstituteResponse> getAll(Long universityId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent()) {
            UserType t = viewer.get().getUserType();
            if (t == UserType.ADMIN || t == UserType.SUPER_ADMIN) {
                var scope = universityScopeService.resolveAdminListScope(viewerEmail, universityId);
                if (scope.allUniversities()) {
                    return instituteRepository.findAll().stream()
                            .map(this::mapToResponse)
                            .sorted(RussianSort.byText(InstituteResponse::getName)
                                    .thenComparing(InstituteResponse::getId, Comparator.nullsLast(Long::compareTo)))
                            .collect(Collectors.toList());
                }
                return instituteRepository.findByUniversityId(scope.universityId()).stream()
                        .map(this::mapToResponse)
                        .sorted(RussianSort.byText(InstituteResponse::getName)
                                .thenComparing(InstituteResponse::getId, Comparator.nullsLast(Long::compareTo)))
                        .collect(Collectors.toList());
            }
        }
        List<Institute> institutes = (universityId != null)
                ? instituteRepository.findByUniversityId(universityId)
                : instituteRepository.findAll();
        return institutes.stream()
                .map(this::mapToResponse)
                .sorted(RussianSort.byText(InstituteResponse::getName)
                        .thenComparing(InstituteResponse::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    @Override
    public InstituteResponse getById(Long id, String viewerEmail) {
        Institute institute = instituteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + id));
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                Long uni = institute.getUniversity().getId();
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            }
        });
        return mapToResponse(institute);
    }

    @Override
    @Transactional
    public InstituteResponse create(InstituteRequest request, String adminEmail) {
        universityScopeService.resolveMutationTargetUniversity(adminEmail, request.getUniversityId());
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + request.getUniversityId()));
        Institute institute = Institute.builder()
                .name(request.getName())
                .shortName(request.getShortName())
                .university(university)
                .build();
        return mapToResponse(instituteRepository.save(institute));
    }

    @Override
    @Transactional
    public InstituteResponse update(Long id, InstituteRequest request, String adminEmail) {
        Institute institute = instituteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + id));
        Long entityUni = institute.getUniversity().getId();
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail, entityUni);
        universityScopeService.assertInstituteInUniversity(id, uni);
        universityScopeService.resolveMutationTargetUniversity(adminEmail, request.getUniversityId());
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + request.getUniversityId()));
        institute.setName(request.getName());
        institute.setShortName(request.getShortName());
        institute.setUniversity(university);
        return mapToResponse(instituteRepository.save(institute));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        Institute institute = instituteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail, institute.getUniversity().getId());
        universityScopeService.assertInstituteInUniversity(id, uni);
        instituteRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private InstituteResponse mapToResponse(Institute institute) {
        return InstituteResponse.builder()
                .id(institute.getId())
                .name(institute.getName())
                .shortName(institute.getShortName())
                .universityId(institute.getUniversity().getId())
                .universityName(institute.getUniversity().getName())
                .build();
    }
}
