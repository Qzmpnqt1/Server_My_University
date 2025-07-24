package org.example.repository;

import org.example.model.RegistrationRequest;
import org.example.model.RegistrationStatus;
import org.example.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Integer> {
    boolean existsByEmailAndStatus(String email, RegistrationStatus status);
    
    List<RegistrationRequest> findByUniversityIdAndStatus(Integer universityId, RegistrationStatus status);
    
    List<RegistrationRequest> findByUniversityIdAndUserTypeAndStatus(
            Integer universityId, UserType userType, RegistrationStatus status);
} 