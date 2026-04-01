package org.example.repository;

import org.example.model.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findByTeacherId(Long teacherId);

    @Query("SELECT ts FROM TeacherSubject ts JOIN ts.teacher t JOIN t.institute i WHERE i.university.id = :uniId")
    List<TeacherSubject> findByTeacherInstituteUniversityId(@Param("uniId") Long universityId);

    boolean existsByTeacherIdAndSubjectId(Long teacherId, Long subjectId);

    @Query("""
            SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END
            FROM TeacherSubject ts
            WHERE ts.teacher.id = :teacherProfileId
            AND EXISTS (SELECT 1 FROM SubjectInDirection sid
                WHERE sid.subject.id = ts.subject.id AND sid.direction.id = :directionId)
            """)
    boolean teacherTeachesInDirection(@Param("teacherProfileId") Long teacherProfileId,
                                      @Param("directionId") Long directionId);
}
