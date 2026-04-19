package com.pesu.ooad.healthcare.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Patient model — maps 1:1 with the UML Class Diagram (Page 4).
 *
 * Fields derived from diagram:
 *   patientId, name, dateOfBirth, contactInfo, insuranceInfo
 *
 * Relationships:
 *   Patient 1 ---* MedicalRecord  (OneToMany, cascade)
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientId;

    @Column(nullable = false)
    private String name;

    private LocalDate dateOfBirth;

    // "contactInfo" from UML (phone / email / address)
    private String contactInfo;

    // "insuranceInfo" from UML
    private String insuranceInfo;

    // One-to-many relationship with MedicalRecord (EHR)
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    // ------------------------------------------------------------------ //
    //  Constructors
    // ------------------------------------------------------------------ //

    public Patient() {}

    public Patient(String name, LocalDate dateOfBirth, String contactInfo, String insuranceInfo) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.contactInfo = contactInfo;
        this.insuranceInfo = insuranceInfo;
    }

    // ------------------------------------------------------------------ //
    //  Getters & Setters
    // ------------------------------------------------------------------ //

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getInsuranceInfo() { return insuranceInfo; }
    public void setInsuranceInfo(String insuranceInfo) { this.insuranceInfo = insuranceInfo; }

    public List<MedicalRecord> getMedicalRecords() { return medicalRecords; }
    public void setMedicalRecords(List<MedicalRecord> medicalRecords) { this.medicalRecords = medicalRecords; }

    // ------------------------------------------------------------------ //
    //  Domain methods (from UML)
    // ------------------------------------------------------------------ //

    /** Update demographics — contact info and insurance (Activity Diagram: updateDemographics) */
    public void updateDemographics(String contactInfo, String insuranceInfo) {
        this.contactInfo = contactInfo;
        this.insuranceInfo = insuranceInfo;
    }
}
