package org.example.repository;

import org.example.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    List<Subject> findAllByOrderByNameAsc();
    
    List<Subject> findByIdIn(List<Integer> ids);
} 