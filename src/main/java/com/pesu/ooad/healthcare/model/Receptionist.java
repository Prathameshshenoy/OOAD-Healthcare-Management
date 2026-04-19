package com.pesu.ooad.healthcare.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("RECEPTIONIST")
public class Receptionist extends User {

    public Receptionist() { super(); }

    public Receptionist(String name, String email, String password) {
        super(name, email, password, "RECEPTIONIST");
    }
}