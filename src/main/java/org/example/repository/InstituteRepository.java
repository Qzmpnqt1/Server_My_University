package org.example.repository;

import org.example.model.Institute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstituteRepository extends JpaRepository<Institute, Integer> {
    List<Institute> findByUniversityIdOrderByNameAsc(Integer universityId);
} 