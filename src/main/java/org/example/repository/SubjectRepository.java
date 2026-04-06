package org.example.repository;

import org.example.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /**
     * Предметы, доступные для привязки в рамках вуза: ещё ни разу не в учебных планах
     * или уже есть в плане направлений этого вуза (но не только в других вузах).
     */
    @Query("""
            SELECT DISTINCT s FROM Subject s
            WHERE NOT EXISTS (SELECT 1 FROM SubjectInDirection sid WHERE sid.subject = s)
               OR EXISTS (
                    SELECT 1 FROM SubjectInDirection sid
                    JOIN sid.direction d
                    JOIN d.institute i
                    WHERE sid.subject = s AND i.university.id = :universityId
               )
            """)
    List<Subject> findAvailableForUniversity(@Param("universityId") Long universityId);
}
