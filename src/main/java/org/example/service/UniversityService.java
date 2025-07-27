package org.example.service;

import org.example.dto.*;

import java.util.List;

public interface UniversityService {
    /**
     * Get all universities
     * 
     * @return List of all universities
     */
    List<UniversityDTO> getAllUniversities();
    
    /**
     * Get institutes by university ID
     * 
     * @param universityId University ID
     * @return List of institutes for the university
     */
    List<InstituteDTO> getInstitutesByUniversity(Integer universityId);
    
    /**
     * Get study directions by institute ID
     * 
     * @param instituteId Institute ID
     * @return List of study directions for the institute
     */
    List<StudyDirectionDTO> getDirectionsByInstitute(Integer instituteId);
    
    /**
     * Get academic groups by direction ID
     * 
     * @param directionId Direction ID
     * @return List of academic groups for the direction
     */
    List<AcademicGroupDTO> getGroupsByDirection(Integer directionId);
    
    /**
     * Get academic groups by direction ID and course
     * 
     * @param directionId Direction ID
     * @param course Course number
     * @return List of academic groups for the direction and course
     */
    List<AcademicGroupDTO> getGroupsByDirectionAndCourse(Integer directionId, Integer course);
    
    /**
     * Get all subjects
     * 
     * @return List of all subjects
     */
    List<SubjectDTO> getAllSubjects();
    
    /**
     * Search subjects by name pattern
     * 
     * @param namePattern Subject name pattern
     * @return List of matching subjects
     */
    List<SubjectDTO> searchSubjectsByName(String namePattern);
    
    /**
     * Get subjects taught in a specific university
     * 
     * @param universityId University ID
     * @return List of subjects taught in the university
     */
    List<SubjectDTO> getSubjectsByUniversity(Integer universityId);
} 