package com.cyberreport.backend.complaint;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByReferenceNumberIgnoreCase(String referenceNumber);

    boolean existsByReferenceNumberIgnoreCase(String referenceNumber);

    List<Complaint> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String userEmail);

    List<Complaint> findAllByOrderByCreatedAtDesc();
}