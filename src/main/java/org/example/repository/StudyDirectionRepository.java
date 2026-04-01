package org.example.repository;

import org.example.model.StudyDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyDirectionRepository extends JpaRepository<StudyDirection, Long> {

    List<StudyDirection> findByInstituteId(Long instituteId);

    List<StudyDirection> findByInstitute_University_Id(Long universityId);
}
