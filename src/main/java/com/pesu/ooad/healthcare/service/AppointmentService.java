package com.pesu.ooad.healthcare.service;

import com.pesu.ooad.healthcare.model.Appointment;
import com.pesu.ooad.healthcare.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;                // ← NEW
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationFacade notificationFacade;
    private final BillingService billingService;                  // ← NEW

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              NotificationFacade notificationFacade,
                              @Lazy BillingService billingService) { // ← NEW (@Lazy breaks the circular dependency)
        this.appointmentRepository = appointmentRepository;
        this.notificationFacade    = notificationFacade;
        this.billingService        = billingService;              // ← NEW
    }

    // MAJOR FEATURE: Book Appointment & Check Availability
    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDate date, LocalTime time) {

        // Prevent booking in the past
        if (date.isBefore(LocalDate.now()) || (date.isEqual(LocalDate.now()) && time.isBefore(LocalTime.now()))) {
            throw new IllegalStateException("Invalid Date/Time: Cannot book appointments in the past.");
        }

        // 1. Check Availability (Prevent Double Booking)
        boolean isSlotTaken = appointmentRepository
                .existsByDoctorIdAndAppointmentDateAndAppointmentTime(doctorId, date, time);
        if (isSlotTaken) {
            throw new IllegalStateException("Slot is unavailable. Doctor already has an appointment at this time.");
        }

        // 2. Create the Appointment Object — confirmed immediately, no separate step needed
        Appointment appointment = new Appointment(patientId, doctorId, date, time);
        appointment.confirm(); // sets status to "Confirmed" right away

        // 3. Save to Database
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 4. Trigger the Facade Pattern
        String message = "Your appointment is confirmed for " + date + " at " + time;
        notificationFacade.notifyUser(patientId, message);
        notificationFacade.notifyUser(doctorId, "New appointment booked for " + date + " at " + time);

        return savedAppointment;
    }

    // ← NEW: Mark a consultation as complete and trigger billing
    /**
     * Called by the receptionist (or doctor) when a consultation is done.
     * Transitions appointment status to "Completed", then hands off to
     * BillingService to create a PENDING_PAYMENT bill.
     *
     * @param appointmentId  the appointment that just finished
     * @param consultationFee  fee to charge the patient
     */
    public Appointment completeAppointment(Long appointmentId, double consultationFee) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        if ("Cancelled".equals(appointment.getStatus())) {
            throw new IllegalStateException("Cannot complete a cancelled appointment.");
        }
        if ("Completed".equals(appointment.getStatus())) {
            throw new IllegalStateException("Appointment #" + appointmentId + " is already completed.");
        }

        // Mark as Completed
        appointment.setStatus("Completed");
        appointmentRepository.save(appointment);

        // Trigger billing — creates the Bill in PENDING_PAYMENT state
        billingService.onConsultationCompleted(appointmentId, consultationFee);

        return appointment;
    }

    // MINOR FEATURE: Update Schedule (Cancel/Block slot)
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        appointment.cancel();
        appointmentRepository.save(appointment);

        notificationFacade.notifyUser(appointment.getPatientId(),
                "Your appointment on " + appointment.getAppointmentDate() + " has been cancelled.");
    }

    public List<Appointment> getDoctorSchedule(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    // ← NEW: used by receptionist/admin who need to see every appointment
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // ← NEW: used by BillingViewController to resolve patientId from appointmentId
    public Appointment getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
    }
}