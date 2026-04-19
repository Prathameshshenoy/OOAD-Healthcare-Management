package com.pesu.ooad.healthcare.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String role;

    // Only populated for RECEPTIONIST — links to their assigned doctor's User.id
    @Column(name = "assigned_doctor_id")
    private Long assignedDoctorId;

    protected User() {}

    protected User(String name, String email, String password, String role) {
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.role     = role;
        this.status   = "Active";
    }

    // UML method: login(): boolean
    public boolean login(String rawPassword) {
        return this.password != null && this.password.equals(rawPassword);
    }

    // State Machine: Active → Inactive
    public void deactivate() { this.status = "Inactive"; }

    // State Machine: Inactive → Active
    public void reactivate() { this.status = "Active"; }

    public boolean isActive() { return "Active".equals(this.status); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getAssignedDoctorId() { return assignedDoctorId; }
    public void setAssignedDoctorId(Long id) { this.assignedDoctorId = id; }
}