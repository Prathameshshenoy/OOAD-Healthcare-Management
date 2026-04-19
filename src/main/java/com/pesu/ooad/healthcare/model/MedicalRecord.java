package com.pesu.ooad.healthcare.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * MedicalRecord (EHR entry) — represents the Electronic Health Record.
 *
 * Per the UML Class Diagram/Activity Diagram:
 *   - Linked to a Patient (ManyToOne)
 *   - Stores consultationNotes, allergies, linkedAppointmentId
 *   - Created via addMedicalRecord() in PatientService
 */
@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    // Owner side of Patient <-> MedicalRecord
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Linked appointment (optional — from activity diagram "fetch past appointments + EHR")
    private Long linkedAppointmentId;

    @Column(columnDefinition = "TEXT")
    private String consultationNotes;

    private String allergies;

    private LocalDate recordDate;

    // ------------------------------------------------------------------ //
    //  Constructors
    // ------------------------------------------------------------------ //

    public MedicalRecord() {}

    public MedicalRecord(Patient patient, Long linkedAppointmentId,
                         String consultationNotes, String allergies, LocalDate recordDate) {
        this.patient = patient;
        this.linkedAppointmentId = linkedAppointmentId;
        this.consultationNotes = consultationNotes;
        this.allergies = allergies;
        this.recordDate = recordDate;
    }

    // ------------------------------------------------------------------ //
    //  Getters & Setters
    // ------------------------------------------------------------------ //

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Long getLinkedAppointmentId() { return linkedAppointmentId; }
    public void setLinkedAppointmentId(Long linkedAppointmentId) { this.linkedAppointmentId = linkedAppointmentId; }

    public String getConsultationNotes() { return consultationNotes; }
    public void setConsultationNotes(String consultationNotes) { this.consultationNotes = consultationNotes; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
}
