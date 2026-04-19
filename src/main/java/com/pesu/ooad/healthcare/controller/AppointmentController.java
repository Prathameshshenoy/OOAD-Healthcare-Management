package com.pesu.ooad.healthcare.controller;

import com.pesu.ooad.healthcare.model.Appointment;          // ← NEW
import com.pesu.ooad.healthcare.model.User;
import com.pesu.ooad.healthcare.repository.UserRepository;
import com.pesu.ooad.healthcare.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;                                        // ← NEW

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    @Autowired
    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository     = userRepository;
    }

    @GetMapping("/book")
    public String showBookingForm(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        List<User> doctors = userRepository.findByRole("DOCTOR");
        model.addAttribute("doctors", doctors);
        return "book-appointment";
    }

    @PostMapping("/book")
    public String submitBooking(
            @RequestParam Long doctorId,
            @RequestParam String date,
            @RequestParam String time,
            HttpSession session,
            Model model) {

        Long patientId = (Long) session.getAttribute("userId");
        if (patientId == null) {
            return "redirect:/login";
        }

        List<User> doctors = userRepository.findByRole("DOCTOR");
        model.addAttribute("doctors", doctors);

        try {
            LocalDate appointmentDate = LocalDate.parse(date);
            LocalTime appointmentTime = LocalTime.parse(time);

            appointmentService.bookAppointment(patientId, doctorId, appointmentDate, appointmentTime);
            model.addAttribute("successMessage", "Appointment booked successfully! Notifications sent.");
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Invalid input. Please check your date and time format.");
        }
        return "book-appointment";
    }

    @GetMapping("/schedule")
    public String viewSchedule(@RequestParam(required = false) Long doctorId, HttpSession session, Model model) {
        String role   = (String) session.getAttribute("userRole");
        Long   userId = (Long)   session.getAttribute("userId");

        if ("ADMIN".equals(role)) {
            // Admin sees ALL appointments
            List<Appointment> all = appointmentService.getAllAppointments();
            model.addAttribute("appointments", all);
            model.addAttribute("canManage", true);
            Map<Long, String> patientNames = new java.util.HashMap<>();
            for (Appointment a : all) {
                if (!patientNames.containsKey(a.getPatientId())) {
                    userRepository.findById(a.getPatientId())
                        .ifPresent(u -> patientNames.put(u.getId(), u.getName()));
                }
            }
            model.addAttribute("patientNames", patientNames);
            return "schedule";
        }

        if ("RECEPTIONIST".equals(role)) {
            // Receptionist sees only their assigned doctor's appointments
            Long assignedDoctorId = userRepository.findById(userId)
                .map(u -> u.getAssignedDoctorId())
                .orElse(null);

            if (assignedDoctorId == null) {
                model.addAttribute("appointments", java.util.Collections.emptyList());
                model.addAttribute("alertMessage", "You are not assigned to any doctor yet. Contact admin.");
                model.addAttribute("canManage", false);
                return "schedule";
            }

            List<Appointment> appts = appointmentService.getDoctorSchedule(assignedDoctorId);
            model.addAttribute("appointments", appts);
            model.addAttribute("canManage", true);
            Map<Long, String> patientNames = new java.util.HashMap<>();
            for (Appointment a : appts) {
                if (!patientNames.containsKey(a.getPatientId())) {
                    userRepository.findById(a.getPatientId())
                        .ifPresent(u -> patientNames.put(u.getId(), u.getName()));
                }
            }
            model.addAttribute("patientNames", patientNames);
            // Show which doctor they're managing
            userRepository.findById(assignedDoctorId)
                .ifPresent(d -> model.addAttribute("assignedDoctorName", d.getName()));
            return "schedule";
        }

        // DOCTOR — read-only view of their own schedule, no action buttons
        if (doctorId == null) {
            doctorId = userId;
        }
        if (doctorId == null) {
            return "redirect:/login";
        }
        model.addAttribute("appointments", appointmentService.getDoctorSchedule(doctorId));
        model.addAttribute("canManage", false); // doctors cannot cancel or complete
        return "schedule";
    }

    @PostMapping("/cancel")
    public String cancelAppointment(@RequestParam Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return "redirect:/appointments/schedule";
    }

    // ← NEW: Mark consultation as complete and trigger billing
    /**
     * Called from the receptionist's billing dashboard when a consultation is done.
     * Accepts appointmentId + consultationFee, marks the appointment "Completed",
     * and hands off to BillingService to create the PENDING_PAYMENT bill.
     *
     * Secured: only ADMIN or RECEPTIONIST may call this.
     * Returns JSON so the billing.html page can handle it via fetch().
     */
    @PostMapping("/complete")
    @ResponseBody
    public Map<String, Object> completeAppointment(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        String role = (String) session.getAttribute("userRole");
        if (role == null || (!"ADMIN".equals(role) && !"RECEPTIONIST".equals(role))) { // DOCTOR intentionally excluded
            return Map.of("error", "Access denied.");
        }

        try {
            Long appointmentId    = Long.valueOf(body.get("appointmentId").toString());
            double consultationFee = Double.parseDouble(body.get("consultationFee").toString());

            Appointment completed = appointmentService.completeAppointment(appointmentId, consultationFee);

            return Map.of(
                "success",       true,
                "appointmentId", completed.getId(),
                "status",        completed.getStatus(),
                "message",       "Appointment #" + completed.getId() + " marked complete. Bill created."
            );
        } catch (IllegalStateException | IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "Unexpected error: " + e.getMessage());
        }
    }

    // AJAX: booked time slots for a given doctor + date
    @GetMapping("/api/booked-times")
    @ResponseBody
    public List<String> getBookedTimes(@RequestParam Long doctorId, @RequestParam String date) {
        LocalDate appointmentDate = LocalDate.parse(date);
        return appointmentService.getDoctorSchedule(doctorId).stream()
                .filter(appt -> appt.getAppointmentDate().equals(appointmentDate)
                             && !"Cancelled".equals(appt.getStatus()))
                .map(appt -> appt.getAppointmentTime().toString().substring(0, 5))
                .toList();
    }
}