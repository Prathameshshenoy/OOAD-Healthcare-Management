package com.pesu.ooad.healthcare.service;

import com.pesu.ooad.healthcare.model.Appointment;
import com.pesu.ooad.healthcare.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationFacade notificationFacade;

    // Spring automatically injects the Repository and your Facade here
    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository, NotificationFacade notificationFacade) {
        this.appointmentRepository = appointmentRepository;
        this.notificationFacade = notificationFacade;
    }

    // MAJOR FEATURE: Book Appointment & Check Availability
    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDate date, LocalTime time) {
        
        // --- NEW LOGIC: Prevent booking in the past ---
        if (date.isBefore(LocalDate.now()) || (date.isEqual(LocalDate.now()) && time.isBefore(LocalTime.now()))) {
            throw new IllegalStateException("Invalid Date/Time: Cannot book appointments in the past.");
        }

        // 1. Check Availability (Prevent Double Booking)
        boolean isSlotTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTime(doctorId, date, time);
        
        if (isSlotTaken) {
            throw new IllegalStateException("Slot is unavailable. Doctor already has an appointment at this time.");
        }

        // 2. Create the Appointment Object
        Appointment appointment = new Appointment(patientId, doctorId, date, time);
        
        // 3. Save to Database
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 4. Trigger the Facade Pattern
        String message = "Your appointment is confirmed for " + date + " at " + time;
        notificationFacade.notifyUser(patientId, message);
        notificationFacade.notifyUser(doctorId, "New appointment booked for " + date + " at " + time);

        return savedAppointment;
    }

    // MINOR FEATURE: Update Schedule (Cancel/Block slot)
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        
        appointment.cancel(); // Changes state to "Cancelled"
        appointmentRepository.save(appointment);
        
        notificationFacade.notifyUser(appointment.getPatientId(), "Your appointment on " + appointment.getAppointmentDate() + " has been cancelled.");
    }

    public List<Appointment> getDoctorSchedule(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }
}