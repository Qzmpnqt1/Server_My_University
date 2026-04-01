package org.example.repository;

import org.example.model.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {

    Optional<AdminProfile> findByUserId(Long userId);

    @Query("""
            SELECT ap FROM AdminProfile ap
            JOIN FETCH ap.university
            WHERE ap.user.id = :userId
            """)
    Optional<AdminProfile> findFetchedByUserId(@Param("userId") Long userId);

    @Query("SELECT ap.user.id FROM AdminProfile ap WHERE ap.university.id = :uniId")
    List<Long> findUserIdsByUniversityId(@Param("uniId") Long universityId);
}
