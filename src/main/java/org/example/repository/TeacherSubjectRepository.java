package org.example.repository;

import org.example.model.Institute;
import org.example.model.StudyDirection;
import org.example.model.SubjectInDirection;
import org.example.model.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findByTeacherId(Long teacherId);

    @Query("""
            SELECT ts FROM TeacherSubject ts
            JOIN ts.teacher t
            WHERE (t.university IS NOT NULL AND t.university.id = :uniId)
               OR (t.institute IS NOT NULL AND t.institute.university.id = :uniId)
            """)
    List<TeacherSubject> findByTeacherInUniversityId(@Param("uniId") Long universityId);

    boolean existsByTeacherIdAndSubjectInDirection_Id(Long teacherId, Long subjectInDirectionId);

    @Query("""
            SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END
            FROM TeacherSubject ts
            WHERE ts.teacher.id = :teacherProfileId
              AND ts.subjectInDirection.direction.id = :directionId
            """)
    boolean teacherTeachesInDirection(@Param("teacherProfileId") Long teacherProfileId,
                                      @Param("directionId") Long directionId);

    @Query("""
            SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END
            FROM TeacherSubject ts
            JOIN ts.subjectInDirection sid
            JOIN sid.direction d
            WHERE ts.teacher.id = :teacherProfileId AND d.institute.id = :instituteId
            """)
    boolean teacherTeachesInInstitute(@Param("teacherProfileId") Long teacherProfileId,
                                      @Param("instituteId") Long instituteId);

    @Query("""
            SELECT DISTINCT i FROM TeacherSubject ts
            JOIN ts.subjectInDirection sid
            JOIN sid.direction d
            JOIN d.institute i
            WHERE ts.teacher.id = :teacherProfileId
            ORDER BY i.name
            """)
    List<Institute> findDistinctInstitutesForTeacher(@Param("teacherProfileId") Long teacherProfileId);

    @Query("""
            SELECT DISTINCT d FROM TeacherSubject ts
            JOIN ts.subjectInDirection sid
            JOIN sid.direction d
            WHERE ts.teacher.id = :teacherProfileId AND d.institute.id = :instituteId
            ORDER BY d.name
            """)
    List<StudyDirection> findDistinctDirectionsForTeacherAndInstitute(
            @Param("teacherProfileId") Long teacherProfileId,
            @Param("instituteId") Long instituteId);

    @Query("""
            SELECT sid FROM TeacherSubject ts
            JOIN ts.subjectInDirection sid
            JOIN FETCH sid.subject
            JOIN FETCH sid.direction
            WHERE ts.teacher.id = :teacherProfileId AND sid.direction.id = :directionId
            ORDER BY sid.subject.name, sid.course, sid.semester
            """)
    List<SubjectInDirection> findSubjectDirectionsForTeacherAndDirection(
            @Param("teacherProfileId") Long teacherProfileId,
            @Param("directionId") Long directionId);
}
