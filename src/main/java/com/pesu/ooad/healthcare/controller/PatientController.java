package com.pesu.ooad.healthcare.controller;

import com.pesu.ooad.healthcare.model.Appointment;
import com.pesu.ooad.healthcare.model.MedicalRecord;
import com.pesu.ooad.healthcare.model.Patient;
import com.pesu.ooad.healthcare.repository.AppointmentRepository;
import com.pesu.ooad.healthcare.repository.UserRepository;
import com.pesu.ooad.healthcare.service.PatientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PatientController — MVC Controller for Patient Management & EHR.
 *
 * Activity Diagram (page 12) endpoints:
 *
 *   GET  /patients/search        → Show search form  (RECEPTIONIST / DOCTOR / ADMIN only)
 *   POST /patients/search        → Search by ID or Name
 *
 *   GET  /patients/{id}/profile  → View patient profile
 *   POST /patients/{id}/update   → Update demographics (contact / insurance)
 *
 *   GET  /patients/{id}/history  → View EHR + fetch past appointments
 *   POST /patients/{id}/record   → Add new medical record  (DOCTOR only)
 *
 * Access rules:
 *   - Search / profile / history: RECEPTIONIST, DOCTOR, ADMIN — OR the patient themselves
 *   - Add medical record: DOCTOR only
 *   - Update demographics: the patient themselves only
 */
@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    @Autowired
    public PatientController(PatientService patientService,
                             AppointmentRepository appointmentRepository,
                             UserRepository userRepository) {
        this.patientService          = patientService;
        this.appointmentRepository   = appointmentRepository;
        this.userRepository          = userRepository;
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private boolean isStaff(String role) {
        return "RECEPTIONIST".equals(role) || "DOCTOR".equals(role) || "ADMIN".equals(role);
    }

    // ------------------------------------------------------------------ //
    //  GET /patients/search  — Show search form (staff only)
    // ------------------------------------------------------------------ //

    @GetMapping("/search")
    public String showSearchForm(HttpSession session, Model model) {
        String role = (String) session.getAttribute("userRole");
        if (!isStaff(role)) {
            return "redirect:/dashboard";
        }
        return "patient-search";
    }

    // ------------------------------------------------------------------ //
    //  POST /patients/search  — Activity Diagram: "Search patient (by ID/Name)"
    // ------------------------------------------------------------------ //

    @PostMapping("/search")
    public String searchPatient(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String name,
            HttpSession session,
            Model model) {

        String role = (String) session.getAttribute("userRole");
        if (!isStaff(role)) {
            return "redirect:/dashboard";
        }

        // Receptionists are scoped to their assigned doctor's patients only
        Long assignedDoctorId = null;
        if ("RECEPTIONIST".equals(role)) {
            Long userId = (Long) session.getAttribute("userId");
            assignedDoctorId = userRepository.findById(userId)
                    .map(u -> u.getAssignedDoctorId())
                    .orElse(null);
            if (assignedDoctorId == null) {
                model.addAttribute("alertMessage",
                    "You are not assigned to any doctor. Contact your administrator.");
                return "patient-search";
            }
        }

        // Search by ID
        if (patientId != null) {
            Optional<Patient> result;
            if (assignedDoctorId != null) {
                result = patientService.searchPatientForDoctor(patientId, assignedDoctorId);
            } else {
                result = patientService.searchPatient(patientId);
            }
            if (result.isEmpty()) {
                model.addAttribute("alertMessage",
                    assignedDoctorId != null
                        ? "No patient found with ID #" + patientId + " under your assigned doctor."
                        : "No patient found with ID #" + patientId);
                return "patient-search";
            }
            return "redirect:/patients/" + result.get().getPatientId() + "/profile";
        }

        // Search by Name
        if (name != null && !name.isBlank()) {
            List<Patient> results;
            if (assignedDoctorId != null) {
                results = patientService.searchPatientByNameForDoctor(name, assignedDoctorId);
            } else {
                results = patientService.searchPatientByName(name);
            }
            if (results.isEmpty()) {
                model.addAttribute("alertMessage",
                    assignedDoctorId != null
                        ? "No patients found matching \"" + name + "\" under your assigned doctor."
                        : "No patients found matching \"" + name + "\"");
                return "patient-search";
            }
            model.addAttribute("patients", results);
            return "patient-search";
        }

        model.addAttribute("alertMessage", "Please enter a Patient ID or Name to search.");
        return "patient-search";
    }

    // ------------------------------------------------------------------ //
    //  GET /patients/{id}/profile  — View patient profile
    //  Accessible by staff OR the patient themselves
    // ------------------------------------------------------------------ //

    @GetMapping("/{id}/profile")
    public String viewPatientProfile(@PathVariable Long id, HttpSession session, Model model) {
        String role   = (String) session.getAttribute("userRole");
        Long   userId = (Long)   session.getAttribute("userId");

        // Staff can view; patient can view their own
        boolean isOwnProfile = "PATIENT".equals(role) && id.equals(userId);
        if (!isStaff(role) && !isOwnProfile) {
            return "redirect:/dashboard";
        }

        // Receptionists may only view patients assigned to their doctor
        if ("RECEPTIONIST".equals(role)) {
            Long assignedDoctorId = userRepository.findById(userId)
                    .map(u -> u.getAssignedDoctorId())
                    .orElse(null);
            if (assignedDoctorId == null ||
                patientService.searchPatientForDoctor(id, assignedDoctorId).isEmpty()) {
                model.addAttribute("alertMessage",
                    "Access denied: this patient is not under your assigned doctor.");
                return "patient-search";
            }
        }

        try {
            Patient patient = patientService.getPatientProfile(id);
            model.addAttribute("patient", patient);
            model.addAttribute("userRole", role);
        } catch (IllegalArgumentException e) {
            model.addAttribute("alertMessage", "Patient not found.");
            return "patient-search";
        }
        return "patient-profile";
    }

    // ------------------------------------------------------------------ //
    //  POST /patients/{id}/update  — Update demographics (patient themselves only)
    // ------------------------------------------------------------------ //

    @PostMapping("/{id}/update")
    public String updatePatientProfile(
            @PathVariable Long id,
            @RequestParam String contactInfo,
            @RequestParam String insuranceInfo,
            HttpSession session,
            Model model) {

        String role   = (String) session.getAttribute("userRole");
        Long   userId = (Long)   session.getAttribute("userId");

        // Only the patient themselves can update their own demographics
        boolean isOwnProfile = "PATIENT".equals(role) && id.equals(userId);
        if (!isOwnProfile && !"ADMIN".equals(role)) {
            model.addAttribute("alertMessage", "You can only update your own profile.");
            return "patient-profile";
        }

        try {
            Patient updated = patientService.updatePatientProfile(id, contactInfo, insuranceInfo);
            model.addAttribute("patient", updated);
            model.addAttribute("userRole", role);
            model.addAttribute("successMessage", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("alertMessage", "Patient not found.");
            return "patient-search";
        }
        return "patient-profile";
    }

    // ------------------------------------------------------------------ //
    //  GET /patients/{id}/history  — Activity Diagram:
    //   "View medical history" → "Fetch past appointments + EHR" → "Display consultations"
    // ------------------------------------------------------------------ //

    @GetMapping("/{id}/history")
    public String viewMedicalHistory(@PathVariable Long id, HttpSession session, Model model) {
        String role   = (String) session.getAttribute("userRole");
        Long   userId = (Long)   session.getAttribute("userId");

        boolean isOwnHistory = "PATIENT".equals(role) && id.equals(userId);
        if (!isStaff(role) && !isOwnHistory) {
            return "redirect:/dashboard";
        }

        // Receptionists may only view history for their assigned doctor's patients
        if ("RECEPTIONIST".equals(role)) {
            Long assignedDoctorId = userRepository.findById(userId)
                    .map(u -> u.getAssignedDoctorId())
                    .orElse(null);
            if (assignedDoctorId == null ||
                patientService.searchPatientForDoctor(id, assignedDoctorId).isEmpty()) {
                model.addAttribute("alertMessage",
                    "Access denied: this patient is not under your assigned doctor.");
                return "patient-search";
            }
        }

        try {
            Patient patient             = patientService.getPatientProfile(id);
            List<MedicalRecord> records = patientService.getMedicalHistory(id);

            // All appointments for this patient (for display)
            List<Appointment> allAppointments = appointmentRepository.findByPatientId(id);

            // Build doctorId -> name lookup
            Map<Long, String> doctorNames = new HashMap<>();
            for (Appointment a : allAppointments) {
                userRepository.findById(a.getDoctorId())
                    .ifPresent(u -> doctorNames.put(u.getId(), u.getName()));
            }

            // Only completed ones for the doctor's "link appointment" dropdown
            List<Appointment> completedAppointments = allAppointments.stream()
                    .filter(a -> "Completed".equals(a.getStatus()))
                    .toList();

            model.addAttribute("patient", patient);
            model.addAttribute("medicalRecords", records);
            model.addAttribute("allAppointments", allAppointments);
            model.addAttribute("doctorNames", doctorNames);
            model.addAttribute("completedAppointments", completedAppointments);
            model.addAttribute("userRole", role);
        } catch (IllegalArgumentException e) {
            model.addAttribute("alertMessage", "Patient not found.");
            return "patient-search";
        }
        return "patient-history";
    }

    // ------------------------------------------------------------------ //
    //  POST /patients/{id}/record  — Activity Diagram:
    //   "Add new medical record (notes/allergies)" → "Update EHR in DB"
    //   DOCTOR only
    // ------------------------------------------------------------------ //

    @PostMapping("/{id}/record")
    public String addMedicalRecord(
            @PathVariable Long id,
            @RequestParam(required = false) Long linkedAppointmentId,
            @RequestParam String consultationNotes,
            @RequestParam(required = false) String allergies,
            HttpSession session,
            Model model) {

        String role = (String) session.getAttribute("userRole");
        if (!"DOCTOR".equals(role) && !"ADMIN".equals(role)) {
            model.addAttribute("alertMessage", "Only doctors can add medical records.");
            return "patient-history";
        }

        try {
            patientService.addMedicalRecord(id, linkedAppointmentId, consultationNotes, allergies);

            // Reload everything for the view
            Patient patient             = patientService.getPatientProfile(id);
            List<MedicalRecord> records = patientService.getMedicalHistory(id);
            List<Appointment> completedAppointments = appointmentRepository
                    .findByPatientId(id).stream()
                    .filter(a -> "Completed".equals(a.getStatus()))
                    .toList();

            model.addAttribute("patient", patient);
            model.addAttribute("medicalRecords", records);
            model.addAttribute("completedAppointments", completedAppointments);
            model.addAttribute("userRole", role);
            model.addAttribute("successMessage", "Medical record added successfully.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("alertMessage", "Patient not found.");
            return "patient-search";
        }
        return "patient-history";
    }
}