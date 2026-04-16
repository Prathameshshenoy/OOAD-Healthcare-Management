package com.pesu.ooad.healthcare.repository;

import com.pesu.ooad.healthcare.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Spring Boot automatically converts this method name into a SQL query:
    // SELECT COUNT(*) > 0 FROM appointments WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ?
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(
            Long doctorId, 
            LocalDate appointmentDate, 
            LocalTime appointmentTime
    );

    // Fetch all appointments for a specific doctor (useful for the Update Schedule use case)
    List<Appointment> findByDoctorId(Long doctorId);
    
    // Fetch all appointments for a specific patient
    List<Appointment> findByPatientId(Long patientId);
}