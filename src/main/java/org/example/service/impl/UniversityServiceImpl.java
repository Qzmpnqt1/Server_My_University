package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.UniversityRequest;
import org.example.dto.response.UniversityResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.University;
import org.example.model.UserType;
import org.example.repository.UniversityRepository;
import org.example.repository.UsersRepository;
import org.example.service.UniversityService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityServiceImpl implements UniversityService {

    private final UniversityRepository universityRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<UniversityResponse> getAll(String viewerEmail) {
        Optional<org.example.model.Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent() && viewer.get().getUserType() == UserType.ADMIN) {
            Long uniId = universityScopeService.requireAdminUniversityId(viewerEmail);
            University u = universityRepository.findById(uniId)
                    .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + uniId));
            return List.of(mapToResponse(u));
        }
        return universityRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UniversityResponse getById(Long id, String viewerEmail) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + id));
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN) {
                Long uniId = universityScopeService.requireAdminUniversityId(viewerEmail);
                universityScopeService.assertUniversityMatches(id, uniId);
            }
        });
        return mapToResponse(university);
    }

    @Override
    @Transactional
    public UniversityResponse create(UniversityRequest request, String adminEmail) {
        universityScopeService.requireAdminUniversityId(adminEmail);
        throw new AccessDeniedException("Создание вуза недоступно администратору вуза");
    }

    @Override
    @Transactional
    public UniversityResponse update(Long id, UniversityRequest request, String adminEmail) {
        Long uniId = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertUniversityMatches(id, uniId);
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + id));
        university.setName(request.getName());
        university.setShortName(request.getShortName());
        university.setCity(request.getCity());
        return mapToResponse(universityRepository.save(university));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        universityScopeService.requireAdminUniversityId(adminEmail);
        throw new AccessDeniedException("Удаление вуза недоступно администратору вуза");
    }

    private Optional<org.example.model.Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private UniversityResponse mapToResponse(University university) {
        return UniversityResponse.builder()
                .id(university.getId())
                .name(university.getName())
                .shortName(university.getShortName())
                .city(university.getCity())
                .build();
    }
}
