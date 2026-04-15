# Clinical Appointment and Healthcare Management System

This repository contains the codebase for the Object Oriented Analysis & Design (OOAD) Mini Project. This system is designed to simplify and automate how patients, doctors, receptionists, and administrators interact with healthcare resources.

## 🛠 Tech Stack & Architecture
To meet the project evaluation guidelines, this application strictly follows the **MVC Architecture**.

* **Language:** Java 21
* **Framework:** Spring Boot 4.0.5 (Spring Web, Spring Data JPA)
* **View/Frontend:** Thymeleaf (Server-side HTML rendering)
* **Database:** H2 Database (Configured for persistent file-based storage)
* **Build Tool:** Maven

### Database Persistence
We are using H2 in persistent file mode. The database runs natively inside the Java application and automatically saves to a local `.db` file in the `/data` directory. This guarantees the project runs identically across macOS, Windows, and Linux without requiring Docker or manual SQL server installations.

---

## 📂 Directory Structure
The repository is scaffolded to enforce a strict Model-View-Controller separation. When creating your classes, place them in the appropriate directories below:

```text
OOAD-Healthcare-Management/
├── src/
│   ├── main/
│   │   ├── java/com/pesu/ooad/healthcare/
│   │   │   ├── HealthcareApplication.java  # Main execution class
│   │   │   ├── controller/                 # MVC Controllers (MainController, etc.)
│   │   │   ├── model/                      # Entities (User, Patient, Appointment, Bill)
│   │   │   ├── repository/                 # Database connection interfaces (Spring Data JPA)
│   │   │   ├── service/                    # Business logic, Factory, and Facade implementations
│   │   │   └── strategy/                   # PaymentStrategy interfaces and implementation classes
│   │   └── resources/
│   │       ├── application.properties      # Database and server configurations
│   │       ├── schema.sql                  # Shared SQL table definitions
│   │       └── templates/                  # Thymeleaf HTML view files
├── .gitignore                          # Ignored files (e.g., target/, .idea/)
├── HELP.md                             # Spring Boot documentation
├── mvnw                                # Maven wrapper script (macOS/Linux)
├── mvnw.cmd                            # Maven wrapper script (Windows)
├── pom.xml                             # Maven project dependencies
└── README.md                           # Project documentation
---

## 🧩 Module Division & Design Patterns
The project requires the implementation of 4 major features and 4 minor features, alongside equal participation in applying design principles. The workload is divided into four independent modules. Each team member owns the full stack (UI, Controller, and Database logic) for their assigned module:

**1. Authentication & Access Control**
* **Major Feature:** Registration and Login routing (Session management).
* **Minor Feature:** Manage Staff Accounts (Admin functionality).
* **Assigned Pattern:** **Factory Pattern** (`UserFactory` for instantiating different user roles).

**2. Patient Management & EHR**
* **Major Feature:** Manage Patient Profiles (CRUD operations).
* **Minor Feature:** Manage Consultations & EHR (Updating medical history).
* **Assigned Pattern:** **Singleton Pattern** (`DatabaseConnection` management for heavy data operations).

**3. Core Appointment Engine**
* **Major Feature:** Book Appointment & Check Availability (Core scheduling logic).
* **Minor Feature:** Update Schedule (Doctor availability/leave).
* **Assigned Pattern:** **Facade Pattern** (`NotificationFacade` for triggering SMS/Email alerts).

**4. Financials & Analytics**
* **Major Feature:** Process Billing & Invoices.
* **Minor Feature:** Generate Analytics Reports (Admin dashboard statistics).
* **Assigned Pattern:** **Strategy Pattern** (`PaymentStrategy` for abstracting cash vs. card processing).

---

## 🚀 Local Setup Instructions

**1. Clone the repository:**
```bash
git clone <YOUR-GITHUB-REPO-URL-HERE>
cd OOAD-Healthcare-Management
```

**2. Open in your IDE:**
Open the project folder in your preferred Java IDE (IntelliJ IDEA, Eclipse, or VS Code). Allow Maven a few moments to resolve and download the dependencies from `pom.xml`.

**3. Run the Application:**
Execute the `HealthcareApplication.java` file.

**4. Access the System:**
* **Web Application:** `http://localhost:8080`
* **H2 Database Console:** `http://localhost:8080/h2-console`
  * **JDBC URL:** `jdbc:h2:file:./data/healthcare_db`
  * **Username:** `admin`
  * **Password:** `password`

---

## 📐 UML & Design Consistency (CRITICAL)
This project is an OOAD implementation, meaning the code **must** strictly reflect the models we have already finalized. Before writing any code for your assigned module, you must open the final project report (`OOAD diagrams.pdf`) and review the diagrams.

* **Class Diagram:** Ensure your Java class names, attributes, and method signatures exactly match the diagram (e.g., if you are building the billing module, your method must be named `processPayment(PaymentStrategy)`).
* **Activity Diagrams:** Follow the exact step-by-step logic and `if/else` branching mapped out in your specific Activity Diagram.
* **State Diagrams:** If your module updates the status of an object (e.g., an Appointment changing from `Pending` to `Confirmed`, or a User Account going from `Active` to `Inactive`), you must strictly adhere to the allowed transitions defined in the State Machine diagrams. Do not invent new states.

---

## 🌿 Git Collaboration Workflow
To prevent code conflicts and maintain a stable application, everyone must work on their own isolated feature branch. **Do not push directly to the `main` branch.**

**1. Create and switch to your feature branch:**
```bash
git checkout -b feature/<your-module-name>
# Example: git checkout -b feature/patient-ehr
```

**2. Stage and commit your work:**
```bash
git add .
git commit -m "feat: implemented patient profile update logic"
```

**3. Push your branch to GitHub:**
```bash
git push -u origin feature/<your-module-name>
```

Once your module is complete, fully tested locally, and verified against the shared schema, create a **Pull Request (PR)** on GitHub to merge your work into the `main` branch.
