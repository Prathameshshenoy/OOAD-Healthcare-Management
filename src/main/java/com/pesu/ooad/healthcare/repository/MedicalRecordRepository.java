package com.pesu.ooad.healthcare.repository;

import com.pesu.ooad.healthcare.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MedicalRecordRepository — JPA Repository for MedicalRecord entity.
 * Activity Diagram: "Fetch past appointments + EHR" → findByPatient_PatientId()
 */
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    /** Fetch all EHR records for a given patient — Activity Diagram "fetch past appointments + EHR" */
    List<MedicalRecord> findByPatient_PatientId(Long patientId);
}
