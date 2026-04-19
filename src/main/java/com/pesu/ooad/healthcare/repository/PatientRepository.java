package com.pesu.ooad.healthcare.repository;

import com.pesu.ooad.healthcare.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PatientRepository — JPA Repository for Patient entity.
 * Used by PatientService to query DatabaseConnection (H2 via JPA).
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** Search by name (case-insensitive, partial match) — Activity Diagram "Search patient by Name" */
    List<Patient> findByNameContainingIgnoreCase(String name);

    /** Search by exact patientId — Activity Diagram "Search patient by ID" */
    Optional<Patient> findByPatientId(Long patientId);
}
