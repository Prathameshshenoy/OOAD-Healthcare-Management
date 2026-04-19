package com.pesu.ooad.healthcare.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Receptionist — maps to the UML subclass of User.
 * assignedDoctorId is inherited from User (single-table inheritance).
 * It is set automatically at registration time by AuthService.
 */
@Entity
@DiscriminatorValue("RECEPTIONIST")
public class Receptionist extends User {

    public Receptionist() { super(); }

    public Receptionist(String name, String email, String password) {
        super(name, email, password, "RECEPTIONIST");
    }
}
