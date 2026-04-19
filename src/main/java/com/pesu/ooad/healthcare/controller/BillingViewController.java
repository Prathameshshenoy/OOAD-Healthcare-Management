package com.pesu.ooad.healthcare.controller;

import com.pesu.ooad.healthcare.model.Appointment;
import com.pesu.ooad.healthcare.model.Bill;
import com.pesu.ooad.healthcare.repository.AppointmentRepository;
import com.pesu.ooad.healthcare.repository.BillRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/billing")
public class BillingViewController {

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;

    @Autowired
    public BillingViewController(BillRepository billRepository, AppointmentRepository appointmentRepository) {
        this.billRepository = billRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/manage")
    public String showBillingDashboard(HttpSession session, Model model) {
        String role = (String) session.getAttribute("userRole");
        if (role == null || (!"ADMIN".equals(role) && !"RECEPTIONIST".equals(role))) {
            return "redirect:/login";
        }

        // Fetch all bills to display in the UI
        List<Bill> allBills = billRepository.findAll();
        
        // Fetch all appointments that might need billing.
        // For simplicity, we are grabbing Confirmed appointments.
        List<Appointment> unbilledAppointments = appointmentRepository.findAll().stream()
                .filter(appt -> "Confirmed".equals(appt.getStatus()))
                // Only keep appointments that don't already have a bill
                .filter(appt -> billRepository.findByAppointmentId(appt.getId()).isEmpty())
                .toList();

        model.addAttribute("bills", allBills);
        model.addAttribute("unbilledAppointments", unbilledAppointments);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userRole", role);

        return "billing";
    }
}
