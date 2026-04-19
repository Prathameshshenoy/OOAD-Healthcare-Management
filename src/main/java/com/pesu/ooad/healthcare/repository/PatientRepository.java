package com.pesu.ooad.healthcare.repository;

import com.pesu.ooad.healthcare.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Returns distinct patients who have at least one appointment with the given doctor.
     * Used by receptionist access control — receptionists may only manage patients
     * whose appointments belong to their assigned doctor.
     */
    @Query("SELECT DISTINCT p FROM Patient p " +
           "WHERE p.patientId IN " +
           "(SELECT a.patientId FROM Appointment a WHERE a.doctorId = :doctorId)")
    List<Patient> findPatientsByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Case-insensitive name search scoped to a specific doctor's patients.
     */
    @Query("SELECT DISTINCT p FROM Patient p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND p.patientId IN " +
           "(SELECT a.patientId FROM Appointment a WHERE a.doctorId = :doctorId)")
    List<Patient> findByNameContainingIgnoreCaseAndDoctorId(
            @Param("name") String name, @Param("doctorId") Long doctorId);
}
