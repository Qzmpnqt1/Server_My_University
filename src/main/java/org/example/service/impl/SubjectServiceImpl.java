package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectRequest;
import org.example.dto.response.SubjectResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Subject;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.SubjectRepository;
import org.example.repository.UsersRepository;
import org.example.service.SubjectService;
import org.example.service.UniversityScopeService;
import org.example.service.UniversityScopeService.AdminQueryScope;
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
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<SubjectResponse> getAll(Long requestUniversityId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent()) {
            UserType t = viewer.get().getUserType();
            if (t == UserType.ADMIN || t == UserType.SUPER_ADMIN) {
                AdminQueryScope scope = universityScopeService.resolveAdminQueryScope(viewerEmail, requestUniversityId);
                if (!scope.globalAllUniversities()) {
                    return subjectRepository.findAvailableForUniversity(scope.universityId()).stream()
                            .map(this::mapToResponse)
                            .sorted(RussianSort.byText(SubjectResponse::getName)
                                    .thenComparing(SubjectResponse::getId, Comparator.nullsLast(Long::compareTo)))
                            .collect(Collectors.toList());
                }
            }
        }
        return subjectRepository.findAll().stream()
                .map(this::mapToResponse)
                .sorted(RussianSort.byText(SubjectResponse::getName)
                        .thenComparing(SubjectResponse::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    @Override
    public SubjectResponse getById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        return mapToResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Subject subject = Subject.builder()
                .name(request.getName())
                .build();
        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        subject.setName(request.getName());
        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found with id: " + id);
        }
        subjectRepository.deleteById(id);
    }

    private SubjectResponse mapToResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .build();
    }
}
