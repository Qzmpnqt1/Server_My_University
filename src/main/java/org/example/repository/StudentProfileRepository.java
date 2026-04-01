package org.example.repository;

import org.example.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    @Query("""
            SELECT sp FROM StudentProfile sp
            JOIN FETCH sp.group
            JOIN FETCH sp.institute
            WHERE sp.user.id = :userId
            """)
    Optional<StudentProfile> findFetchedByUserId(@Param("userId") Long userId);

    List<StudentProfile> findByGroupId(Long groupId);

    @Query("SELECT sp.user.id FROM StudentProfile sp WHERE sp.institute.university.id = :uniId")
    List<Long> findUserIdsByUniversityId(@Param("uniId") Long universityId);
}
