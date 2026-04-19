package com.pesu.ooad.healthcare.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pesu.ooad.healthcare.model.User;
import com.pesu.ooad.healthcare.repository.UserRepository;
import com.pesu.ooad.healthcare.model.Patient;
import com.pesu.ooad.healthcare.repository.PatientRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final NotificationFacade notificationFacade;
    private final PatientRepository patientRepository;

    @Autowired
    public AuthService(UserRepository userRepository,
                       UserFactory userFactory,
                       NotificationFacade notificationFacade,
                       PatientRepository patientRepository) {
        this.userRepository     = userRepository;
        this.userFactory        = userFactory;
        this.notificationFacade = notificationFacade;
        this.patientRepository  = patientRepository;
    }

    // =========================
    // REGISTRATION
    // =========================
    public User registerUser(String type, String name, String email, String password) {

        // Check duplicate email
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            throw new IllegalStateException("This email is already registered. Please log in.");
        }

        // Receptionist: auto-assign to the first doctor who has no receptionist yet.
        // Block registration entirely if no such doctor exists.
        Long autoAssignedDoctorId = null;
        if ("RECEPTIONIST".equalsIgnoreCase(type)) {
            User unassignedDoctor = findUnassignedDoctor()
                .orElseThrow(() -> new IllegalStateException(
                    "No doctors are currently available for assignment. " +
                    "A receptionist can only register when at least one doctor has no receptionist assigned."));
            autoAssignedDoctorId = unassignedDoctor.getId();
        }

        // Factory Pattern — create the right User subclass
        User newUser = userFactory.createUser(type, name, email, password);

        // Persist first so we get an ID
        User saved = userRepository.save(newUser);

        // Apply auto-assignment for receptionists
        if (autoAssignedDoctorId != null) {
            saved.setAssignedDoctorId(autoAssignedDoctorId);
            saved = userRepository.save(saved);
        }

        // If registering as a patient, create a linked Patient medical record.
        if ("PATIENT".equalsIgnoreCase(type)) {
            Patient patientRecord = new Patient(
                saved.getId(),
                name,
                null,
                null,
                null
            );
            patientRepository.save(patientRecord);
        }

        // Facade Pattern — welcome notification
        notificationFacade.notifyUser(
            saved.getId(),
            "Welcome to the Healthcare System, " + saved.getName() + "!"
        );

        return saved;
    }

    // =========================
    // LOGIN
    // =========================
    public User login(String email, String password) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("No account found with this email address.");
        }

        User user = optionalUser.get();

        if (!user.isActive()) {
            throw new IllegalArgumentException("Your account has been deactivated. Contact an administrator.");
        }

        if (!user.login(password)) {
            throw new IllegalStateException("Incorrect password. Please try again.");
        }

        return user;
    }

    // =========================
    // ADMIN - GET ALL USERS
    // =========================
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // =========================
    // ADMIN - DEACTIVATE USER
    // =========================
    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.deactivate();
        User saved = userRepository.save(user);
        notificationFacade.notifyUser(saved.getId(), "Your account has been deactivated.");
        return saved;
    }

    // =========================
    // ADMIN - REACTIVATE USER
    // =========================
    public User reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.reactivate();
        User saved = userRepository.save(user);
        notificationFacade.notifyUser(saved.getId(), "Your account has been reactivated.");
        return saved;
    }

    // =========================
    // GET USER BY ID
    // =========================
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    // =========================
    // RECEPTIONIST SLOT CHECK
    // Used by register page to show/hide the Receptionist option.
    // =========================
    public boolean hasUnassignedDoctor() {
        return findUnassignedDoctor().isPresent();
    }

    // Returns the first active doctor who has no receptionist assigned yet.
    private Optional<User> findUnassignedDoctor() {
        List<User> doctors = userRepository.findByRole("DOCTOR");
        return doctors.stream()
            .filter(d -> "Active".equals(d.getStatus()))
            .filter(d -> userRepository.findByRoleAndAssignedDoctorId("RECEPTIONIST", d.getId()).isEmpty())
            .findFirst();
    }
}
