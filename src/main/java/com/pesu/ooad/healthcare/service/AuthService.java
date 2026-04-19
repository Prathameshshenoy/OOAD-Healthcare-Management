package com.pesu.ooad.healthcare.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pesu.ooad.healthcare.model.User;
import com.pesu.ooad.healthcare.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final NotificationFacade notificationFacade;

    @Autowired
    public AuthService(UserRepository userRepository,
                       UserFactory userFactory,
                       NotificationFacade notificationFacade) {
        this.userRepository     = userRepository;
        this.userFactory        = userFactory;
        this.notificationFacade = notificationFacade;
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

        // Factory Pattern
        User newUser = userFactory.createUser(type, name, email, password);

        // Save user
        User saved = userRepository.save(newUser);

        // Facade Pattern (correct method signature)
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

        notificationFacade.notifyUser(
            saved.getId(),
            "Your account has been deactivated."
        );

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

        notificationFacade.notifyUser(
            saved.getId(),
            "Your account has been reactivated."
        );

        return saved;
    }

    // =========================
    // GET USER BY ID
    // =========================
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}