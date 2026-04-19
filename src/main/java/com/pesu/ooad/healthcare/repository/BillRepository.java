package com.pesu.ooad.healthcare.repository;

import com.pesu.ooad.healthcare.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Bill} entities.
 *
 * Spring Boot automatically generates all standard CRUD implementations
 * at runtime — no boilerplate SQL needed.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    /**
     * Finds all bills associated with a given appointment.
     * Useful for checking whether billing has already been triggered.
     */
    List<Bill> findByAppointmentId(Long appointmentId);

    /**
     * Retrieves the most recent (or sole) bill for a given appointment.
     * Returns an Optional to force null-safety at the call site.
     */
    Optional<Bill> findFirstByAppointmentIdOrderByIdDesc(Long appointmentId);
}
