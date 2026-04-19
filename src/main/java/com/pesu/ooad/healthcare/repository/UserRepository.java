package com.pesu.ooad.healthcare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pesu.ooad.healthcare.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(String role);

    /** Find the receptionist assigned to a specific doctor */
    java.util.Optional<User> findByRoleAndAssignedDoctorId(String role, Long assignedDoctorId);
}