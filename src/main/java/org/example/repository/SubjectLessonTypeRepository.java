package org.example.repository;

import org.example.model.SubjectLessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectLessonTypeRepository extends JpaRepository<SubjectLessonType, Long> {

    List<SubjectLessonType> findBySubjectDirectionId(Long subjectDirectionId);

    @Query("SELECT slt FROM SubjectLessonType slt JOIN slt.subjectDirection sd JOIN sd.direction d JOIN d.institute i WHERE i.university.id = :uniId")
    List<SubjectLessonType> findByUniversityId(@Param("uniId") Long universityId);
}
