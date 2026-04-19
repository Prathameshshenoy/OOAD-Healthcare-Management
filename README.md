# Clinical Appointment and Healthcare Management System

> **UE23CS352B — Object Oriented Analysis & Design | Mini Project**
> PES University · Department of Computer Science and Engineering · Jan–May 2026

A full-stack healthcare management web application built strictly on **OOAD principles** and the **MVC architectural pattern**. The system automates clinical workflows for patients, doctors, receptionists, and administrators — covering appointment scheduling, electronic health records, billing, and staff management.

**Team:** Arkul Prathamesh Shenoy · Arjun MS · Aryan Arun Hiremath · Atharw Rawal
**Guide:** Prof. Vikram Dunga

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 (Spring Web, Spring Data JPA) |
| View | Thymeleaf (server-side HTML rendering) |
| Database | H2 (persistent file-based, no external server needed) |
| Build | Maven |

---

## 🚀 Running the Application

**1. Clone the repository**
```bash
git clone https://github.com/Prathameshshenoy/OOAD-Healthcare-Management.git
cd OOAD-Healthcare-Management
```

**2. Run**
```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```
Or open in IntelliJ / Eclipse and run `HealthcareApplication.java` directly.

**3. Open in browser**
```
http://localhost:8080
```

**4. H2 Database Console** (optional inspection)
```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/healthcare_db
Username: admin
Password: password
```

---

## 📂 Project Structure

```
src/main/java/com/pesu/ooad/healthcare/
├── HealthcareApplication.java       # Entry point
├── controller/                      # MVC Controllers
│   ├── MainController.java          # Auth, registration, dashboard, admin staff
│   ├── AppointmentController.java   # Booking, schedule, cancel, complete
│   ├── PatientController.java       # Patient search, profile, EHR/history
│   ├── BillingController.java       # Billing API
│   └── BillingViewController.java   # Billing UI
├── model/                           # Domain entities (UML Class Diagram)
│   ├── User.java                    # Abstract base (id, name, email, role, status)
│   ├── Admin.java
│   ├── Doctor.java
│   ├── Receptionist.java            # Inherits assignedDoctorId from User
│   ├── PatientUser.java
│   ├── Patient.java                 # Medical profile (EHR data)
│   ├── Appointment.java             # Core scheduling entity
│   ├── Bill.java                    # Invoice + payment
│   ├── BillingStatus.java
│   └── MedicalRecord.java           # EHR consultation records
├── repository/                      # Spring Data JPA interfaces
├── service/                         # Business logic
│   ├── AuthService.java             # Registration, login, staff management
│   ├── AppointmentService.java
│   ├── PatientService.java
│   ├── BillingService.java
│   ├── DatabaseConnection.java      # Singleton Pattern
│   ├── UserFactory.java             # Factory Pattern
│   └── NotificationFacade.java      # Facade Pattern
└── strategy/                        # Strategy Pattern
    ├── PaymentStrategy.java
    ├── CashPayment.java
    └── CreditCardPayment.java

src/main/resources/
├── application.properties
└── templates/                       # Thymeleaf views
    ├── login.html
    ├── register.html
    ├── dashboard.html
    ├── admin-staff.html
    ├── book-appointment.html
    ├── schedule.html
    ├── patient-search.html
    ├── patient-profile.html
    ├── patient-history.html
    └── billing.html
```

---

## 🧩 Modules & Design Patterns

### I. Authentication & Access Control
Handles registration, login, session management, and admin staff operations.

- **Role-Based Access Control (RBAC):** Four roles — `PATIENT`, `DOCTOR`, `RECEPTIONIST`, `ADMIN` — each routed to their own dashboard view with enforced access guards on every endpoint.
- **Staff Management:** Admins can activate and deactivate any user account, matching the User Account Lifecycle state machine (`Unregistered → Active → LoggedIn / Inactive`).
- **Receptionist Auto-Assignment:** When a receptionist registers, the system automatically assigns them to the first active doctor who has no receptionist yet. If no such doctor exists, registration is blocked with a clear error. The register form disables the Receptionist option entirely when no slots are available.
- **Design Pattern — Factory:** `UserFactory.createUser(type, ...)` centralises instantiation of all user subclasses, keeping `AuthService` free of `new Doctor(...)` / `new Receptionist(...)` calls.

### II. Patient Management & EHR
Covers patient profile CRUD and electronic health record management.

- **Patient Search:** Staff can search by Patient ID or name. Receptionists are scoped — they can only find and view patients who have at least one appointment with their assigned doctor.
- **Profile Management:** Patients update their own contact and insurance details. Doctors and admins have read access.
- **Medical History:** Doctors add consultation notes and allergy records linked to completed appointments. Full EHR history is viewable by the patient and clinical staff.
- **Design Pattern — Singleton:** Every database operation in `PatientService` calls `DatabaseConnection.getInstance()` before querying, ensuring a single shared connection instance throughout the application lifecycle.

### III. Core Appointment Engine
Handles scheduling, availability checking, and doctor roster management.

- **Booking with Conflict Prevention:** Appointments are validated for past-date booking and double-booking before being saved. On success, `NotificationFacade` simulates an SMS/email alert.
- **Role-scoped Schedule View:**
  - **Doctor** — read-only view of their own upcoming appointments.
  - **Receptionist** — full manage view, but only for their assigned doctor's appointments.
  - **Admin** — full view of all appointments across all doctors.
- **Appointment Lifecycle:** Matches the state machine: `Pending → Confirmed → InConsultation → Completed → Billed → Paid` (or `→ Cancelled` from Pending/Confirmed).
- **Design Pattern — Facade:** `NotificationFacade.notifyUser()` provides a single method call that internally delegates to `sendEmail()` and `sendSMS()`, hiding the complexity of two separate notification subsystems from the core appointment logic.

### IV. Financials & Analytics
Covers invoice generation, payment processing, and admin reporting.

- **Automated Billing:** Marking an appointment complete triggers `Bill.generateInvoice()`, creating a bill tied to that consultation. Billing state machine: `Unbilled → InvoiceGenerated → PendingPayment → Processing → Paid`.
- **Payment Processing:** Patients (or receptionists on their behalf) choose Cash or Card at checkout. The correct strategy is injected at runtime without modifying the `Bill` class.
- **Admin Dashboard:** Revenue totals, appointment volumes, and operational stats aggregated across all doctors.
- **Design Pattern — Strategy:** `PaymentStrategy` interface with `CashPayment` and `CreditCardPayment` implementations. `Bill.processPayment(PaymentStrategy)` switches algorithms at runtime, and new payment types (e.g. UPI, Insurance) can be added without touching existing code — Open/Closed Principle in practice.

---

## 🎭 User Roles & Access Summary

| Feature | Patient | Doctor | Receptionist | Admin |
|---|:---:|:---:|:---:|:---:|
| Book appointment | ✅ | — | ✅ | — |
| View own schedule | — | ✅ | ✅ (assigned doctor only) | ✅ (all) |
| Cancel appointment | — | — | ✅ | ✅ |
| View patient profile | Own only | ✅ | ✅ (assigned doctor's patients) | ✅ |
| Add medical record / EHR | — | ✅ | — | ✅ |
| Process billing | — | — | ✅ | ✅ |
| Manage staff accounts | — | — | — | ✅ |
| Generate analytics | — | — | — | ✅ |

---

## 🏗 SOLID Principles Applied

| Principle | Where |
|---|---|
| **Single Responsibility** | `Bill` handles only invoicing; `Appointment` handles only scheduling state |
| **Open / Closed** | New payment types extend `PaymentStrategy` without modifying `Bill` |
| **Dependency Inversion** | `Bill` depends on the `PaymentStrategy` abstraction, not on `CreditCardPayment` directly |
| **Encapsulate What Varies** | Role-conditional instantiation logic is fully encapsulated inside `UserFactory` |

---

## 🔐 Test Accounts (seed data)

The H2 database in `/data/` is pre-seeded. You can also register fresh accounts through the UI.

Suggested registration order for a clean test:
1. Register a **Doctor**
2. Register a **Receptionist** (auto-assigned to the doctor above)
3. Register a **Patient**
4. Register an **Admin**

---

## 📄 License

Academic project — PES University, 2026. Not licensed for commercial use.