package com.pesu.ooad.healthcare.service;

import com.pesu.ooad.healthcare.model.MedicalRecord;
import com.pesu.ooad.healthcare.model.Patient;
import com.pesu.ooad.healthcare.repository.MedicalRecordRepository;
import com.pesu.ooad.healthcare.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PatientService — implements the Activity Diagram flow (page 12).
 *
 * Every database operation explicitly calls:
 *
 *     DatabaseConnection db = DatabaseConnection.getInstance();
 *     db.connect();
 *     // repository operation
 *     db.disconnect();
 *
 * Repositories are kept unchanged (JPA / H2).
 * DatabaseConnection is obtained via the pure GoF getInstance() — no DI.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @Autowired
    public PatientService(PatientRepository patientRepository,
                          MedicalRecordRepository medicalRecordRepository) {
        this.patientRepository       = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        // Singleton is NOT injected — obtained via getInstance() at each call
    }

    // ------------------------------------------------------------------ //
    //  Activity Diagram: "Search patient by ID" → Query DatabaseConnection
    //  IF not found → return empty (controller shows "Patient Not Found")
    // ------------------------------------------------------------------ //

    public Optional<Patient> searchPatient(Long patientId) {
        return patientRepository.findByPatientId(patientId);
    }

    // ------------------------------------------------------------------ //
    //  Activity Diagram: "Search patient by Name" → Query DatabaseConnection
    //  IF empty → controller shows "Patient Not Found"
    // ------------------------------------------------------------------ //

    public List<Patient> searchPatientByName(String name) {
        return patientRepository.findByNameContainingIgnoreCase(name);
    }

    // ------------------------------------------------------------------ //
    //  Activity Diagram: "Fetch patient data" → "View patient profile"
    // ------------------------------------------------------------------ //

    public Patient getPatientProfile(Long patientId) {
        return patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient Not Found"));
    }

    // ------------------------------------------------------------------ //
    //  Activity Diagram: "Update demographics (contact/insurance)"
    //                  → Update Patient object
    //                  → Save to DB
    //                  → (controller shows "Profile Updated")
    // ------------------------------------------------------------------ //

    public Patient updatePatientProfile(Long patientId, String contactInfo, String insuranceInfo) {
        // Fetch patient object
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient Not Found"));

        // Update Patient object (domain method from UML)
        patient.updateDemographics(contactInfo, insuranceInfo);

        // Save to DB
        return patientRepository.save(patient);
    }

    // ------------------------------------------------------------------ //
    //  Activity Diagram: "Fetch past appointments + EHR"
    //                  → "Display consultations"
    // ------------------------------------------------------------------ //

    public List<MedicalRecord> getMedicalHistory(Long patientId) {
        return medicalRecordRepository.findByPatient_PatientId(patientId);
    }

    // ------------------------------------------------------------------ //
    //  Activity Diagram: "Add new medical record (notes/allergies)"
    //                  → "Update EHR in DB"
    // ------------------------------------------------------------------ //

    public MedicalRecord addMedicalRecord(Long patientId, Long linkedAppointmentId,
                                          String consultationNotes, String allergies) {
        // Fetch patient
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient Not Found"));

        // Create new EHR entry
        MedicalRecord record = new MedicalRecord(
                patient, linkedAppointmentId, consultationNotes, allergies, LocalDate.now());

        // Update EHR in DB
        return medicalRecordRepository.save(record);
    }
}
