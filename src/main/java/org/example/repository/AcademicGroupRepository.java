package org.example.repository;

import org.example.model.AcademicGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicGroupRepository extends JpaRepository<AcademicGroup, Long> {

    List<AcademicGroup> findByDirectionId(Long directionId);

    List<AcademicGroup> findByDirection_Institute_University_Id(Long universityId);
}
