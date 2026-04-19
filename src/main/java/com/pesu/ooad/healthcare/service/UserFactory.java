package com.pesu.ooad.healthcare.service;

import org.springframework.stereotype.Component;

import com.pesu.ooad.healthcare.model.Admin;
import com.pesu.ooad.healthcare.model.Doctor;
import com.pesu.ooad.healthcare.model.PatientUser;
import com.pesu.ooad.healthcare.model.Receptionist;
import com.pesu.ooad.healthcare.model.User;

@Component
public class UserFactory {

    public User createUser(String type, String name, String email, String password) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("User type cannot be null or empty.");
        }

        return switch (type.toUpperCase()) {
            case "PATIENT"      -> new PatientUser(name, email, password);
            case "DOCTOR"       -> new Doctor(name, email, password);
            case "RECEPTIONIST" -> new Receptionist(name, email, password);
            case "ADMIN"        -> new Admin(name, email, password);
            default -> throw new IllegalArgumentException(
                "Unknown user type: '" + type + "'");
        };
    }
}