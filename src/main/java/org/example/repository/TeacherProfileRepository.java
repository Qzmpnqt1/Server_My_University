package org.example.repository;

import org.example.model.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Integer> {
    boolean existsByUserId(Integer userId);
} 