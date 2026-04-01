package org.example.repository;

import org.example.model.SubjectPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectPracticeRepository extends JpaRepository<SubjectPractice, Long> {

    List<SubjectPractice> findBySubjectDirectionId(Long subjectDirectionId);
}
