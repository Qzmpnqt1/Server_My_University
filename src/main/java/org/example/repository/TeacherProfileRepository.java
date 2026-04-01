package org.example.repository;

import org.example.model.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    Optional<TeacherProfile> findByUserId(Long userId);

    @Query("""
            SELECT tp FROM TeacherProfile tp
            LEFT JOIN FETCH tp.institute
            WHERE tp.user.id = :userId
            """)
    Optional<TeacherProfile> findFetchedByUserId(@Param("userId") Long userId);

    @Query("SELECT tp.user.id FROM TeacherProfile tp WHERE tp.institute IS NOT NULL AND tp.institute.university.id = :uniId")
    List<Long> findUserIdsByUniversityId(@Param("uniId") Long universityId);
}
