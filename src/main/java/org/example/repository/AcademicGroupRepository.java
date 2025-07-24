package org.example.repository;

import org.example.model.AcademicGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicGroupRepository extends JpaRepository<AcademicGroup, Integer> {
    List<AcademicGroup> findByDirectionIdOrderByNameAsc(Integer directionId);
    
    List<AcademicGroup> findByDirectionIdAndCourseOrderByNameAsc(Integer directionId, Integer course);
} 