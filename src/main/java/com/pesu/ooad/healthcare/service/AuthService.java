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

    // REGISTRATION
    public User registerUser(String type, String name, String email, String password) {

        // Factory Pattern
        User newUser = userFactory.createUser(type, name, email, password);

        // Singleton Pattern
        DatabaseConnection db = DatabaseConnection.getInstance();
        db.connect();

        // Check duplicate email
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            db.disconnect();
            throw new IllegalStateException("This email is already registered. Please log in.");
        }

        User saved = userRepository.save(newUser);
        db.disconnect();

        // Facade Pattern - send welcome notification
        notificationFacade.notifyUser(
            saved.getId(),
            "Welcome to the Healthcare System, " + saved.getName() + "!"
        );

        return saved;
    }

    // LOGIN
    public User login(String email, String password) {

        DatabaseConnection db = DatabaseConnection.getInstance();
        db.connect();

        Optional<User> optionalUser = userRepository.findByEmail(email);
        db.disconnect();

        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("No account found with this email address.");
        }

        User user = optionalUser.get();

        if (!user.isActive()) {
            throw new IllegalArgumentException("Your account has been deactivated. Contact an administrator.");
        }

        // UML method: User.login()
        if (!user.login(password)) {
            throw new IllegalStateException("Incorrect password. Please try again.");
        }

        return user;
    }

    // ADMIN - Get all users
    public List<User> getAllUsers() {
        DatabaseConnection db = DatabaseConnection.getInstance();
        db.connect();
        List<User> users = userRepository.findAll();
        db.disconnect();
        return users;
    }

    // ADMIN - Deactivate (State Machine: Active → Inactive)
    public User deactivateUser(Long userId) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        db.connect();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.deactivate();
        User saved = userRepository.save(user);
        db.disconnect();

        notificationFacade.notifyUser(saved.getId(), "Your account has been deactivated.");
        return saved;
    }

    // ADMIN - Reactivate (State Machine: Inactive → Active)
    public User reactivateUser(Long userId) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        db.connect();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.reactivate();
        User saved = userRepository.save(user);
        db.disconnect();

        notificationFacade.notifyUser(saved.getId(), "Your account has been reactivated.");
        return saved;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}