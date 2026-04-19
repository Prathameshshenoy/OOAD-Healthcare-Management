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

> **Suggested registration order for a clean test run:**
> Doctor → Receptionist (auto-assigned to the doctor above) → Patient → Admin

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
    ├── login.html / register.html / dashboard.html
    ├── admin-staff.html
    ├── book-appointment.html / schedule.html
    ├── patient-search.html / patient-profile.html / patient-history.html
    └── billing.html
```

---

## 🧩 Modules & Feature Overview

### I. Authentication & Access Control
- **RBAC:** Four roles — `PATIENT`, `DOCTOR`, `RECEPTIONIST`, `ADMIN` — each routed to their own dashboard with enforced access guards on every endpoint.
- **Staff Management:** Admins activate/deactivate accounts, matching the User Account Lifecycle state machine (`Unregistered → Active ↔ Inactive`).
- **Receptionist Auto-Assignment:** On registration, a receptionist is automatically assigned to the first active doctor with no receptionist yet. Registration is blocked if no such doctor exists. The register form disables the option and explains why.

### II. Patient Management & EHR
- **Scoped Search:** Receptionists can only search and view patients who have appointments with their assigned doctor. Doctors and admins see all patients.
- **Profile Management:** Patients manage their own contact and insurance details. Staff have read access.
- **Medical History:** Doctors add consultation notes and allergy records linked to completed appointments. Full EHR history is viewable by the patient and clinical staff.

### III. Core Appointment Engine
- **Booking with Conflict Prevention:** Validates against past-date booking and double-booking before saving. On success, `NotificationFacade` sends alerts to both patient and doctor.
- **Role-Scoped Schedule:** Doctors see their own appointments (read-only). Receptionists manage only their assigned doctor's appointments. Admins see all.
- **Appointment Lifecycle:** `Pending → Confirmed → InConsultation → Completed → Billed → Paid` (or `→ Cancelled`).

### IV. Financials & Analytics
- **Automated Billing:** Completing an appointment triggers `Bill.generateInvoice()`. Billing lifecycle: `Unbilled → InvoiceGenerated → PendingPayment → Processing → Paid`.
- **Payment Processing:** Cash or Card selected at checkout; the correct `PaymentStrategy` is injected at runtime.
- **Admin Dashboard:** Revenue totals, appointment volumes, and operational statistics.

---

## 🎭 Role & Access Matrix

| Feature | Patient | Doctor | Receptionist | Admin |
|---|:---:|:---:|:---:|:---:|
| Book appointment | ✅ | — | ✅ | — |
| View schedule | — | ✅ own | ✅ assigned doctor | ✅ all |
| Cancel appointment | — | — | ✅ | ✅ |
| View patient profile | Own only | ✅ | ✅ assigned doctor's patients | ✅ |
| Add EHR / medical record | — | ✅ | — | ✅ |
| Process billing | — | — | ✅ | ✅ |
| Manage staff accounts | — | — | — | ✅ |
| Generate analytics | — | — | — | ✅ |

---

## 🏗 Design Principles

### GRASP Principles

**Creator**
A class is assigned the responsibility of creating an object if it aggregates or closely uses that object. `UserFactory` creates all `User` subclass instances — it is the single place in the system that knows how to construct a `Doctor`, `Receptionist`, `Patient`, or `Admin`. `AuthService` creates `Patient` medical records immediately after a patient user is saved, because `AuthService` has the information needed (the new user's ID and name) and initiates the operation.

**Information Expert**
Responsibility is assigned to the class that has the information needed to fulfil it. `Appointment` owns `confirm()` and `cancel()` because it holds the state being changed. `Bill` owns `generateInvoice()`, `markPendingPayment()`, `startProcessing()`, and `processPayment()` because it holds `amount` and `status` — the data those methods operate on.

**Low Coupling**
Classes are connected through abstractions rather than concrete types wherever variation is expected. `Bill` depends on the `PaymentStrategy` interface, not on `CreditCardPayment` or `CashPayment` directly. `AppointmentService` calls `NotificationFacade.notifyUser()` rather than reaching into email or SMS subsystems. Controllers depend on service interfaces, not repositories.

**Controller**
A dedicated non-UI class handles system events. `MainController` receives and routes all authentication and staff management requests. `AppointmentController`, `PatientController`, `BillingController`, and `BillingViewController` each handle a distinct subsystem, keeping request-handling logic separated from both the view (Thymeleaf templates) and the model (domain classes).

**High Cohesion**
Each class has a focused, related set of responsibilities. `PatientService` only performs patient-related data operations. `BillingService` only handles invoice and payment logic. `NotificationFacade` only deals with sending alerts. No service class mixes concerns from two different modules.

**Polymorphism**
Behaviour that varies by type is handled through polymorphic dispatch rather than `if/else` chains. `PaymentStrategy` defines `pay(double amount)` and `CashPayment` / `CreditCardPayment` each provide their own implementation. `Bill.processPayment(strategy)` calls `strategy.pay(amount)` — it does not know or care which concrete type is passed. The `User` class hierarchy (`Doctor`, `Receptionist`, `PatientUser`, `Admin`) similarly allows role-based behaviour to be extended without modifying the base class.

**Pure Fabrication**
`DatabaseConnection`, `UserFactory`, and `NotificationFacade` do not correspond to any real-world domain concept in the problem domain — they are invented classes whose sole purpose is to assign responsibilities that don't naturally belong anywhere else, keeping the domain model clean.

**Indirection**
`NotificationFacade` introduces an intermediary between the appointment/billing logic and the underlying SMS and email subsystems. Neither `AppointmentService` nor `BillingService` knows that two separate notification channels exist — they call one method and the facade handles routing. This means notification channels can be added, removed, or changed without touching any business logic class.

**Protected Variations**
Variation points are wrapped behind stable interfaces. The payment mechanism is a known variation point — it is protected behind `PaymentStrategy` so that adding UPI or insurance billing in the future requires only a new class, not a change to `Bill`. The notification channels are a variation point — protected behind `NotificationFacade`. The user creation logic is a variation point — protected behind `UserFactory`.

---

### SOLID Principles

**Single Responsibility Principle (SRP)**
Each class has exactly one reason to change. `Bill` changes only if invoice/payment logic changes. `Appointment` changes only if scheduling data or state transitions change. `NotificationFacade` changes only if notification behaviour changes. Controllers change only if routing logic changes. No class mixes domain logic with persistence or UI concerns.

**Open/Closed Principle (OCP)**
The payment module is open for extension and closed for modification. To add a new payment type (e.g. UPI), you create a new class implementing `PaymentStrategy` — `Bill`, `BillingService`, and all callers remain entirely untouched. The same applies to user roles: adding a new role means adding a subclass of `User` and a branch in `UserFactory`, without modifying any existing model or service class.

**Liskov Substitution Principle (LSP)**
Every `User` subclass (`Doctor`, `Receptionist`, `PatientUser`, `Admin`) can be used wherever a `User` is expected without breaking the program. `AuthService.login()` accepts any `User` and calls `user.login(password)` — it works identically regardless of which concrete subclass is returned by the repository. Every `PaymentStrategy` implementation can be passed to `Bill.processPayment()` interchangeably.

**Interface Segregation Principle (ISP)**
`PaymentStrategy` is a minimal single-method interface (`pay(double): boolean`). Implementors are not forced to implement methods they don't need. Repositories (`AppointmentRepository`, `PatientRepository`, `UserRepository`, etc.) extend `JpaRepository` and only declare the specific query methods their callers actually require, rather than exposing a monolithic data-access surface.

**Dependency Inversion Principle (DIP)**
High-level modules do not depend on low-level concrete implementations. `Bill` depends on the `PaymentStrategy` abstraction — not on `CreditCardPayment`. Controllers depend on service classes — not on repositories directly. `AppointmentService` depends on `NotificationFacade` — not on the email or SMS implementation classes.

---

## 🎨 Design Patterns

### Creational Patterns

#### Singleton — `DatabaseConnection`
```
service/DatabaseConnection.java
```
`DatabaseConnection` follows the pure GoF Singleton structure as required by the UML class diagram: a `private static` instance field, a `private` constructor that prevents external instantiation, and a `public static synchronized getInstance()` method that performs lazy initialisation. Every database operation in `PatientService` obtains the connection via `DatabaseConnection.getInstance()` before calling the repository and releases it after — ensuring exactly one connection object exists for the application's lifetime and preventing concurrent connection leaks.

```java
public static synchronized DatabaseConnection getInstance() {
    if (instance == null) {
        instance = new DatabaseConnection();
    }
    return instance;
}
```

#### Factory — `UserFactory`
```
service/UserFactory.java
```
`UserFactory.createUser(type, name, email, password)` encapsulates all user-object construction logic in one place. `AuthService` calls the factory rather than using `new Doctor(...)` or `new Receptionist(...)` directly — keeping the registration flow free of type-conditional instantiation code. Adding a new role requires only a new `case` in the factory's `switch` statement; no other class needs to change.

```java
return switch (type.toUpperCase()) {
    case "PATIENT"      -> new PatientUser(name, email, password);
    case "DOCTOR"       -> new Doctor(name, email, password);
    case "RECEPTIONIST" -> new Receptionist(name, email, password);
    case "ADMIN"        -> new Admin(name, email, password);
    default -> throw new IllegalArgumentException("Unknown user type: " + type);
};
```

---

### Structural Patterns

#### Facade — `NotificationFacade`
```
service/NotificationFacade.java
```
`NotificationFacade` provides a single simplified interface — `notifyUser(userId, message)` — over two underlying subsystems: `sendEmail()` and `sendSMS()`. Both `AppointmentService` and `AuthService` call this one method; neither knows that two channels exist or how they work. The subsystem methods are `private`, making the facade the only access point. In a production system, swapping from SMS to push notifications would require changes only inside `NotificationFacade`.

```java
public void notifyUser(Long userId, String message) {
    sendEmail(userId, message);  // private — hidden from callers
    sendSMS(userId, message);    // private — hidden from callers
}
```

---

### Behavioral Patterns

#### Strategy — `PaymentStrategy`
```
strategy/PaymentStrategy.java
strategy/CashPayment.java
strategy/CreditCardPayment.java
```
`PaymentStrategy` defines the algorithm interface with a single method `pay(double amount): boolean`. `CashPayment` and `CreditCardPayment` are the concrete strategies. `Bill.processPayment(PaymentStrategy)` delegates execution entirely to the injected strategy — no `if (type == "cash")` branching exists in `Bill`. The strategy to use is selected at the controller/service level at runtime based on the user's checkout choice. New payment methods (UPI, insurance) require only a new implementing class.

```java
// Bill.java — closed for modification, open for extension
public boolean processPayment(PaymentStrategy strategy) {
    boolean success = strategy.pay(this.amount);
    this.status = success ? BillingStatus.PAID : BillingStatus.PENDING_PAYMENT;
    return success;
}
```

---

## ⚠️ Anti-Patterns Identified & Avoided

### Project Management Anti-Pattern — *God Class* (avoided)
A God Class is a single class that accumulates too many responsibilities, becoming the central hub that everything else depends on. An early draft of this project had a single `HealthcareService` class handling authentication, appointment booking, patient management, and billing in one file. This was refactored into `AuthService`, `AppointmentService`, `PatientService`, and `BillingService` — each with a single focused responsibility — before any code was committed.

### Architecture Anti-Pattern — *Bypassing the MVC Layers* (avoided)
An architecture anti-pattern common in Spring projects is having controllers directly call JPA repositories, skipping the service layer entirely. This collapses the Model and Controller layers and makes business logic untestable in isolation. In this project, controllers (`AppointmentController`, `PatientController`, etc.) always call a service class. No controller imports or injects a repository directly.

### Development Anti-Pattern — *Magic Numbers / Hard-Coded Strings* (avoided)
Billing status transitions in early drafts used raw string comparisons like `if (status.equals("billed"))` scattered across multiple classes, making state management fragile and hard to change. This was replaced with the `BillingStatus` enum, so all status values are defined once and referenced by name. Similarly, database credentials are declared as named constants in `DatabaseConnection` rather than being repeated across the codebase.

---

## 📄 License

Academic project — PES University, 2026. Not licensed for commercial use.
