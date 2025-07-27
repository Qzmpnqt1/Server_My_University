package org.example.repository;

import org.example.model.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Integer> {
    List<TeacherSubject> findByTeacherId(Integer teacherId);
    List<TeacherSubject> findBySubjectId(Integer subjectId);
    void deleteByTeacherId(Integer teacherId);
} 