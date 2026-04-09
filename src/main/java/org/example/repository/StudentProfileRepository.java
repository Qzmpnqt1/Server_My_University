package org.example.repository;

import org.example.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    @Query("""
            SELECT sp FROM StudentProfile sp
            JOIN FETCH sp.group
            JOIN FETCH sp.institute i
            JOIN FETCH i.university
            WHERE sp.user.id = :userId
            """)
    Optional<StudentProfile> findFetchedByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT sp FROM StudentProfile sp
            JOIN FETCH sp.user
            JOIN FETCH sp.group g
            JOIN FETCH g.direction
            WHERE sp.user.id IN :userIds
            """)
    List<StudentProfile> findFetchedByUserIdIn(@Param("userIds") Collection<Long> userIds);

    List<StudentProfile> findByGroupId(Long groupId);

    @Query("SELECT sp.user.id FROM StudentProfile sp WHERE sp.institute.university.id = :uniId")
    List<Long> findUserIdsByUniversityId(@Param("uniId") Long universityId);
}
