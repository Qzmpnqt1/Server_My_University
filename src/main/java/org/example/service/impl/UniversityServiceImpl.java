package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.UniversityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityServiceImpl implements UniversityService {
    private final UniversityRepository universityRepository;
    private final InstituteRepository instituteRepository;
    private final StudyDirectionRepository studyDirectionRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UniversityDTO> getAllUniversities() {
        return universityRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToUniversityDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstituteDTO> getInstitutesByUniversity(Integer universityId) {
        return instituteRepository.findByUniversityIdOrderByNameAsc(universityId).stream()
                .map(this::mapToInstituteDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyDirectionDTO> getDirectionsByInstitute(Integer instituteId) {
        return studyDirectionRepository.findByInstituteIdOrderByNameAsc(instituteId).stream()
                .map(this::mapToDirectionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicGroupDTO> getGroupsByDirection(Integer directionId) {
        return academicGroupRepository.findByDirectionIdOrderByNameAsc(directionId).stream()
                .map(this::mapToAcademicGroupDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicGroupDTO> getGroupsByDirectionAndCourse(Integer directionId, Integer course) {
        return academicGroupRepository.findByDirectionIdAndCourseOrderByNameAsc(directionId, course).stream()
                .map(this::mapToAcademicGroupDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectDTO> getAllSubjects() {
        return subjectRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToSubjectDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectDTO> searchSubjectsByName(String namePattern) {
        return subjectRepository.findAllByOrderByNameAsc().stream()
                .filter(subject -> subject.getName().toLowerCase().contains(namePattern.toLowerCase()))
                .map(this::mapToSubjectDTO)
                .collect(Collectors.toList());
    }

    // Mapping methods
    private UniversityDTO mapToUniversityDTO(University university) {
        return UniversityDTO.builder()
                .id(university.getId())
                .name(university.getName())
                .shortName(university.getShortName())
                .city(university.getCity())
                .build();
    }

    private InstituteDTO mapToInstituteDTO(Institute institute) {
        return InstituteDTO.builder()
                .id(institute.getId())
                .name(institute.getName())
                .shortName(institute.getShortName())
                .universityId(institute.getUniversity().getId())
                .build();
    }

    private StudyDirectionDTO mapToDirectionDTO(StudyDirection direction) {
        return StudyDirectionDTO.builder()
                .id(direction.getId())
                .name(direction.getName())
                .code(direction.getCode())
                .instituteId(direction.getInstitute().getId())
                .build();
    }

    private AcademicGroupDTO mapToAcademicGroupDTO(AcademicGroup group) {
        return AcademicGroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .course(group.getCourse())
                .yearOfAdmission(group.getYearOfAdmission())
                .directionId(group.getDirection().getId())
                .build();
    }

    private SubjectDTO mapToSubjectDTO(Subject subject) {
        return SubjectDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .build();
    }
} 