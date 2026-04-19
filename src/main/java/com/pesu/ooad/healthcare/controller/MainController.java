package com.pesu.ooad.healthcare.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pesu.ooad.healthcare.model.User;
import com.pesu.ooad.healthcare.service.AuthService;

import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    private final AuthService authService;

    @Autowired
    public MainController(AuthService authService) {
        this.authService = authService;
    }

    // Redirect root to login
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    // ── REGISTRATION ──────────────────────────────────────────────────
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String submitRegistration(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role,
            Model model) {
        try {
            authService.registerUser(role, name, email, password);
            model.addAttribute("successMessage", "Registration successful! Please log in.");
            return "login";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    // UML method: handleLogin(user: String, pass: String)
    @PostMapping("/login")
    public String handleLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {
        try {
            User user = authService.login(email, password);

            // Store in session — other modules read these
            session.setAttribute("userId",   user.getId());
            session.setAttribute("userRole", user.getRole());
            session.setAttribute("userName", user.getName());

            // UML method: routeRequest(action: String)
            return routeRequest(user.getRole());

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "login";
        }
    }

    // UML method: routeRequest(action: String)
    private String routeRequest(String role) {
        return "redirect:/dashboard";
    }

    // ── DASHBOARD ─────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            return "redirect:/login";
        }
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userRole", role);
        return "dashboard";
    }

    // ── LOGOUT ────────────────────────────────────────────────────────
    // State Machine: LoggedIn → Active
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ── ADMIN: MANAGE STAFF ───────────────────────────────────────────
    @GetMapping("/admin/staff")
    public String viewStaffAccounts(HttpSession session, Model model) {
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) {
            model.addAttribute("errorMessage", "Access denied. Admin only.");
            return "dashboard";
        }
        List<User> users = authService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("userName", session.getAttribute("userName"));
        return "admin-staff";
    }

    // State Machine: Active → Inactive
    @PostMapping("/admin/staff/deactivate")
    public String deactivateUser(@RequestParam Long userId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) return "redirect:/login";
        authService.deactivateUser(userId);
        return "redirect:/admin/staff";
    }

    // State Machine: Inactive → Active
    @PostMapping("/admin/staff/reactivate")
    public String reactivateUser(@RequestParam Long userId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) return "redirect:/login";
        authService.reactivateUser(userId);
        return "redirect:/admin/staff";
    }
}