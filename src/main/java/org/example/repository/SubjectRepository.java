package org.example.repository;

import org.example.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    List<Subject> findAllByOrderByNameAsc();
    
    List<Subject> findByIdIn(List<Integer> ids);
    
    /**
     * Находит все предметы, которые преподаются в университете с указанным ID
     * Это предметы, которые связаны с направлениями обучения институтов этого университета
     */
    @Query("SELECT DISTINCT s FROM Subject s " +
           "JOIN s.subjectsInDirection sd " +
           "JOIN sd.direction d " +
           "JOIN d.institute i " +
           "WHERE i.university.id = :universityId " +
           "ORDER BY s.name ASC")
    List<Subject> findByUniversityId(@Param("universityId") Integer universityId);
} 