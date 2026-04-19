package com.pesu.ooad.healthcare.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * PatientUser = login account for a patient.
 * Kept separate from Patient.java (medical data entity)
 * to avoid breaking Arjun's module.
 */
@Entity
@DiscriminatorValue("PATIENT")
public class PatientUser extends User {

    public PatientUser() { super(); }

    public PatientUser(String name, String email, String password) {
        super(name, email, password, "PATIENT");
    }
}