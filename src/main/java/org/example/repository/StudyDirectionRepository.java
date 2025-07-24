package org.example.repository;

import org.example.model.StudyDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyDirectionRepository extends JpaRepository<StudyDirection, Integer> {
    List<StudyDirection> findByInstituteIdOrderByNameAsc(Integer instituteId);
} 