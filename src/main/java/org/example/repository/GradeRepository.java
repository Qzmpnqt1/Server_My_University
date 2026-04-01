package org.example.repository;

import org.example.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByStudentIdAndSubjectDirectionId(Long studentId, Long subjectDirectionId);

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findBySubjectDirectionId(Long subjectDirectionId);

    List<Grade> findByStudentIdAndSubjectDirectionIdIn(Long studentId, List<Long> subjectDirectionIds);
}
