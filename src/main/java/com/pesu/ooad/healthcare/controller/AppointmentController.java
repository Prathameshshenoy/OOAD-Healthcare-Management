package com.pesu.ooad.healthcare.controller;

import com.pesu.ooad.healthcare.model.User;
import com.pesu.ooad.healthcare.repository.UserRepository;
import com.pesu.ooad.healthcare.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository; // Brings in your friend's database access

    @Autowired
    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/book")
    public String showBookingForm(HttpSession session, Model model) {
        // Kick them to the login page if they try to access this without signing in
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        
        // Fetch all doctors and send them to the HTML for the dropdown menu
        List<User> doctors = userRepository.findByRole("DOCTOR"); 
        model.addAttribute("doctors", doctors);
        
        return "book-appointment"; 
    }

    @PostMapping("/book")
    public String submitBooking(
            @RequestParam Long doctorId, // Patient ID is gone from here!
            @RequestParam String date,
            @RequestParam String time,
            HttpSession session, // Injects the active session
            Model model) {
            
        // 1. Securely pull the patient ID from their login session
        Long patientId = (Long) session.getAttribute("userId");
        if (patientId == null) {
            return "redirect:/login";
        }

        // 2. Re-add doctors to the model in case the page needs to reload
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
        // If a doctor looks at their schedule, grab their own ID from the session automatically
        if (doctorId == null) {
            doctorId = (Long) session.getAttribute("userId");
        }
        
        if (doctorId == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("appointments", appointmentService.getDoctorSchedule(doctorId));
        return "schedule"; 
    }

    @PostMapping("/cancel")
    public String cancelAppointment(@RequestParam Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return "redirect:/appointments/schedule"; 
    }
    
    // --- NEW: AJAX Endpoint for dynamic time slots ---
    @GetMapping("/api/booked-times")
    @ResponseBody
    public List<String> getBookedTimes(@RequestParam Long doctorId, @RequestParam String date) {
        LocalDate appointmentDate = LocalDate.parse(date);
        
        // Filter only appointments for the selected date that are NOT cancelled
        return appointmentService.getDoctorSchedule(doctorId).stream()
                .filter(appt -> appt.getAppointmentDate().equals(appointmentDate) && !"Cancelled".equals(appt.getStatus()))
                .map(appt -> appt.getAppointmentTime().toString().substring(0, 5)) // Extract "HH:MM"
                .toList();
    }
}