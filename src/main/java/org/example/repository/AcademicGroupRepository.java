package org.example.repository;

import org.example.model.AcademicGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicGroupRepository extends JpaRepository<AcademicGroup, Long> {

    List<AcademicGroup> findByDirectionId(Long directionId);

    List<AcademicGroup> findByDirection_Institute_University_Id(Long universityId);

    @Query("""
            SELECT g FROM AcademicGroup g
            JOIN FETCH g.direction d
            JOIN FETCH d.institute i
            WHERE i.university.id = :uniId
              AND (:instituteId IS NULL OR i.id = :instituteId)
              AND (:directionId IS NULL OR d.id = :directionId)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY i.name, d.name, g.name
            """)
    List<AcademicGroup> searchForCompareCatalog(
            @Param("uniId") Long universityId,
            @Param("instituteId") Long instituteId,
            @Param("directionId") Long directionId,
            @Param("q") String q);
}
