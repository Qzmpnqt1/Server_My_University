package org.example.repository;

import org.example.model.RegistrationRequest;
import org.example.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long>,
        JpaSpecificationExecutor<RegistrationRequest> {

    boolean existsByEmailAndStatus(String email, RegistrationStatus status);

    List<RegistrationRequest> findByStatus(RegistrationStatus status);

    Optional<RegistrationRequest> findFirstByEmailOrderByCreatedAtDesc(String email);
}
