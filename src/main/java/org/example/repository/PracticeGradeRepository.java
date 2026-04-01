package org.example.repository;

import org.example.model.PracticeGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeGradeRepository extends JpaRepository<PracticeGrade, Long> {

    Optional<PracticeGrade> findByStudentIdAndPracticeId(Long studentId, Long practiceId);

    List<PracticeGrade> findByStudentId(Long studentId);

    List<PracticeGrade> findByPracticeId(Long practiceId);

    List<PracticeGrade> findByPracticeIdIn(List<Long> practiceIds);

    List<PracticeGrade> findByStudentIdAndPracticeIdIn(Long studentId, List<Long> practiceIds);
}
