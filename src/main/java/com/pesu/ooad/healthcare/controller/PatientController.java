package com.pesu.ooad.healthcare.controller;

import com.pesu.ooad.healthcare.model.MedicalRecord;
import com.pesu.ooad.healthcare.model.Patient;
import com.pesu.ooad.healthcare.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * PatientController — MVC Controller for Patient Management & EHR.
 *
 * Endpoints map 1:1 to the Activity Diagram (page 12):
 *
 *   GET  /patients/search        → Show search form
 *   POST /patients/search        → Search by ID or Name
 *                                   └─ NOT FOUND → showAlert("Patient Not Found")
 *                                   └─ FOUND     → redirect to profile
 *
 *   GET  /patients/{id}/profile  → View patient profile
 *
 *   POST /patients/{id}/update   → Update demographics (contact/insurance)
 *                                   └─ showAlert("Profile Updated")
 *
 *   GET  /patients/{id}/history  → View medical history (fetch EHR + consultations)
 *
 *   POST /patients/{id}/record   → Add new medical record (notes/allergies)
 */
@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // ------------------------------------------------------------------ //
    //  GET /patients/search  — Show search form
    // ------------------------------------------------------------------ //

    @GetMapping("/search")
    public String showSearchForm() {
        return "patient-search";
    }

    // ------------------------------------------------------------------ //
    //  POST /patients/search  — Activity Diagram: "Search patient (by ID/Name)"
    //                           → Query DatabaseConnection
    //                           → IF not found: showAlert("Patient Not Found")
    //                           → ELSE: Fetch patient data / View profile
    // ------------------------------------------------------------------ //

    @PostMapping("/search")
    public String searchPatient(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String name,
            Model model) {

        // Search by ID
        if (patientId != null) {
            Optional<Patient> result = patientService.searchPatient(patientId);
            if (result.isEmpty()) {
                // Activity Diagram: showAlert("Patient Not Found")
                model.addAttribute("alertMessage", "Patient Not Found");
                return "patient-search";
            }
            // ELSE: view patient profile
            model.addAttribute("patient", result.get());
            return "redirect:/patients/" + result.get().getPatientId() + "/profile";
        }

        // Search by Name
        if (name != null && !name.isBlank()) {
            List<Patient> results = patientService.searchPatientByName(name);
            if (results.isEmpty()) {
                model.addAttribute("alertMessage", "Patient Not Found");
                return "patient-search";
            }
            model.addAttribute("patients", results);
            return "patient-search";
        }

        model.addAttribute("alertMessage", "Please enter a Patient ID or Name");
        return "patient-search";
    }

    // ------------------------------------------------------------------ //
    //  GET /patients/{id}/profile  — View patient profile
    // ------------------------------------------------------------------ //

    @GetMapping("/{id}/profile")
    public String viewPatientProfile(@PathVariable Long id, Model model) {
        try {
            Patient patient = patientService.getPatientProfile(id);
            model.addAttribute("patient", patient);
        } catch (IllegalArgumentException e) {
            // Activity Diagram: showAlert("Patient Not Found")
            model.addAttribute("alertMessage", "Patient Not Found");
            return "patient-search";
        }
        return "patient-profile";
    }

    // ------------------------------------------------------------------ //
    //  POST /patients/{id}/update  — Activity Diagram:
    //   "Update demographics (contact/insurance)" → Update Patient object
    //   → Save to DB → showAlert("Profile Updated")
    // ------------------------------------------------------------------ //

    @PostMapping("/{id}/update")
    public String updatePatientProfile(
            @PathVariable Long id,
            @RequestParam String contactInfo,
            @RequestParam String insuranceInfo,
            Model model) {
        try {
            Patient updated = patientService.updatePatientProfile(id, contactInfo, insuranceInfo);
            model.addAttribute("patient", updated);
            // Activity Diagram: showAlert("Profile Updated")
            model.addAttribute("successMessage", "Profile Updated");
        } catch (IllegalArgumentException e) {
            model.addAttribute("alertMessage", "Patient Not Found");
            return "patient-search";
        }
        return "patient-profile";
    }

    // ------------------------------------------------------------------ //
    //  GET /patients/{id}/history  — Activity Diagram:
    //   "View medical history" → "Fetch past appointments + EHR"
    //   → "Display consultations"
    // ------------------------------------------------------------------ //

    @GetMapping("/{id}/history")
    public String viewMedicalHistory(@PathVariable Long id, Model model) {
        try {
            Patient patient = patientService.getPatientProfile(id);
            List<MedicalRecord> records = patientService.getMedicalHistory(id);
            model.addAttribute("patient", patient);
            model.addAttribute("medicalRecords", records);
        } catch (IllegalArgumentException e) {
            model.addAttribute("alertMessage", "Patient Not Found");
            return "patient-search";
        }
        return "patient-history";
    }

    // ------------------------------------------------------------------ //
    //  POST /patients/{id}/record  — Activity Diagram:
    //   "Add new medical record (notes/allergies)" → "Update EHR in DB"
    // ------------------------------------------------------------------ //

    @PostMapping("/{id}/record")
    public String addMedicalRecord(
            @PathVariable Long id,
            @RequestParam(required = false) Long linkedAppointmentId,
            @RequestParam String consultationNotes,
            @RequestParam String allergies,
            Model model) {
        try {
            patientService.addMedicalRecord(id, linkedAppointmentId, consultationNotes, allergies);
            List<MedicalRecord> records = patientService.getMedicalHistory(id);
            Patient patient = patientService.getPatientProfile(id);
            model.addAttribute("patient", patient);
            model.addAttribute("medicalRecords", records);
            model.addAttribute("successMessage", "Medical Record Added Successfully");
        } catch (IllegalArgumentException e) {
            model.addAttribute("alertMessage", "Patient Not Found");
            return "patient-search";
        }
        return "patient-history";
    }
}
