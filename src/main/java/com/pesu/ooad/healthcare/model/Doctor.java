package com.pesu.ooad.healthcare.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DOCTOR")
public class Doctor extends User {

    public Doctor() { super(); }

    public Doctor(String name, String email, String password) {
        super(name, email, password, "DOCTOR");
    }
}