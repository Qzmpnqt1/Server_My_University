package org.example.repository;

import org.example.model.SubjectInDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectInDirectionRepository extends JpaRepository<SubjectInDirection, Long> {

    List<SubjectInDirection> findByDirectionId(Long directionId);

    List<SubjectInDirection> findBySubjectId(Long subjectId);

    List<SubjectInDirection> findByDirectionIdAndSemester(Long directionId, Integer semester);

    List<SubjectInDirection> findByDirection_Institute_University_Id(Long universityId);
}
