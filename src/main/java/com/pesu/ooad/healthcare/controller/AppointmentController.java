package com.pesu.ooad.healthcare.controller;

import com.pesu.ooad.healthcare.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/book")
    public String showBookingForm() {
        return "book-appointment"; 
    }

    @PostMapping("/book")
    public String submitBooking(
            @RequestParam Long patientId,
            @RequestParam Long doctorId,
            @RequestParam String date,
            @RequestParam String time,
            Model model) {
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
    public String viewSchedule(@RequestParam(required = false, defaultValue = "1") Long doctorId, Model model) {
        model.addAttribute("appointments", appointmentService.getDoctorSchedule(doctorId));
        return "schedule"; 
    }

    @PostMapping("/cancel")
    public String cancelAppointment(@RequestParam Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return "redirect:/appointments/schedule"; 
    }
}