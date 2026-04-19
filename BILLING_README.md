# Feature 4 — Billing & Invoice Cycle

> **Module owner:** Financials & Analytics  
> **Design pattern:** Strategy Pattern (`PaymentStrategy`)  
> **UML reference:** `OOAD diagrams.pdf` — Class Diagram (p.4), State Machine (p.7), Activity Diagram (p.9)

---

## 1. Overview

This module implements the full **billing lifecycle** for the healthcare system — from the moment a consultation ends to the moment a patient receives a payment receipt. Every class, method name, and state transition is derived directly from the UML diagrams.

---

## 2. Files Added

```
src/main/java/com/pesu/ooad/healthcare/
│
├── model/
│   ├── BillingStatus.java          ← Enum — 5 lifecycle states
│   └── Bill.java                   ← JPA Entity — core billing object
│
├── strategy/
│   ├── PaymentStrategy.java        ← Interface — Strategy Pattern contract
│   ├── CreditCardPayment.java      ← Concrete strategy — card payments
│   └── CashPayment.java            ← Concrete strategy — cash payments
│
├── repository/
│   └── BillRepository.java         ← Spring Data JPA interface
│
├── service/
│   └── BillingService.java         ← Business logic — orchestrates the full flow
│
└── controller/
    └── BillingController.java      ← REST API — HTTP entry points
```

---

## 3. How Each File Works

### 3.1 `model/BillingStatus.java` — The Five States

```java
public enum BillingStatus {
    UNBILLED,
    INVOICE_GENERATED,
    PENDING_PAYMENT,
    PROCESSING,
    PAID
}
```

Directly encodes the **State Machine Diagram (p.7)**. The enum is used as the `status` column on the `Bill` entity (`@Enumerated(EnumType.STRING)`), so the H2 database stores the human-readable name (e.g., `"PENDING_PAYMENT"`).

---

### 3.2 `model/Bill.java` — Core Entity & State Guard

The `Bill` class is the central object. It:

- Is a **JPA `@Entity`** persisted to the `bills` table
- Holds the `amount` (double) and `appointmentId` (FK)
- **Owns all state transition logic** — no other layer can bypass the guards

#### Attributes

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Auto-generated primary key |
| `appointmentId` | `Long` | FK to the triggering appointment |
| `amount` | `double` | Consultation fee to charge |
| `status` | `BillingStatus` | Current lifecycle state |

#### UML-mandated methods

| Method | UML Source | State Transition |
|---|---|---|
| `generateInvoice()` | Class Diagram (p.4) | `UNBILLED → INVOICE_GENERATED` |
| `processPayment(PaymentStrategy)` | Class Diagram (p.4) | `PROCESSING → PAID` |

#### Additional state-transition methods

These are required to implement the full State Machine (p.7) but are called exclusively by `BillingService`, not the controller:

| Method | State Transition |
|---|---|
| `markPendingPayment()` | `INVOICE_GENERATED → PENDING_PAYMENT` |
| `startProcessing()` | `PENDING_PAYMENT → PROCESSING` |

**Every transition method throws `IllegalStateException` if called out of sequence**, making invalid jumps impossible at runtime:

```java
public void generateInvoice() {
    if (this.status != BillingStatus.UNBILLED) {
        throw new IllegalStateException("generateInvoice() can only be called when status is UNBILLED.");
    }
    this.status = BillingStatus.INVOICE_GENERATED;
}
```

#### Strategy Pattern in `processPayment`

```java
public boolean processPayment(PaymentStrategy strategy) {
    if (this.status != BillingStatus.PROCESSING) {
        throw new IllegalStateException(...);
    }
    boolean success = strategy.pay(this.amount);  // delegates — no if/else here
    if (success) this.status = BillingStatus.PAID;
    return success;
}
```

`Bill` has **zero knowledge** of credit cards or cash — it only knows the `PaymentStrategy` interface. New payment methods (e.g., UPI) require zero changes to `Bill`.

---

### 3.3 `strategy/PaymentStrategy.java` — The Interface

```java
public interface PaymentStrategy {
    boolean pay(double amount);
}
```

Maps exactly to the UML interface on the Class Diagram (p.4). Returns `boolean` so that a declined payment can be handled without exceptions.

---

### 3.4 `strategy/CreditCardPayment.java` & `strategy/CashPayment.java` — Concrete Strategies

Both are `@Component` Spring beans. Spring injects them into `BillingService` by type — no manual `new` calls anywhere.

| Bean | `@Component` name | `pay()` behaviour |
|---|---|---|
| `CreditCardPayment` | `"creditCardPayment"` | Simulates gateway auth; logs to console; returns `true` |
| `CashPayment` | `"cashPayment"` | Simulates counter collection; logs to console; returns `true` |

In production, you would replace the body of `pay()` with a real gateway SDK call (e.g., Razorpay, Stripe) without touching any other file.

---

### 3.5 `repository/BillRepository.java`

```java
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill>     findByAppointmentId(Long appointmentId);
    Optional<Bill> findFirstByAppointmentIdOrderByIdDesc(Long appointmentId);
}
```

Spring Boot auto-generates SQL from the method names at startup. No manual queries needed:

| Method | Generated SQL |
|---|---|
| `findByAppointmentId` | `SELECT * FROM bills WHERE appointment_id = ?` |
| `findFirstByAppointmentIdOrderByIdDesc` | `SELECT * FROM bills WHERE appointment_id = ? ORDER BY id DESC LIMIT 1` |

---

### 3.6 `service/BillingService.java` — The Orchestrator

This is where the **Activity Diagram (p.9)** is implemented as code. The service calls `Bill`'s methods in the correct order and coordinates with `NotificationFacade`.

#### Key public methods

| Method | What it does |
|---|---|
| `generateInvoiceForAppointment(appointmentId, amount)` | Creates bill → `generateInvoice()` → notifies user → `markPendingPayment()` |
| `processPayment(billId, paymentMethod)` | `startProcessing()` → resolves strategy → `bill.processPayment(strategy)` → notifies user |
| `onConsultationCompleted(appointmentId, amount)` | Integration stub — called by `AppointmentService` |
| `getBillById(billId)` | Read-only lookup |
| `getLatestBillForAppointment(appointmentId)` | Read-only lookup |

#### `resolveStrategy` — the only if/else for payment type

```java
private PaymentStrategy resolveStrategy(String paymentMethod) {
    return switch (paymentMethod.toUpperCase()) {
        case "CARD" -> creditCardPayment;
        case "CASH" -> cashPayment;
        default -> throw new IllegalArgumentException("Unknown payment method: ...");
    };
}
```

This is the **only place** in the codebase that maps a string to a payment type. `Bill.processPayment()` never sees this string — it only receives the resolved strategy bean.

#### Integration with AppointmentService (Module 3)

When Module 3 marks a consultation as complete, it should call:

```java
// Inside AppointmentService — after setting appointment status to "Completed":
billingService.onConsultationCompleted(appointment.getId(), consultationFee);
```

This requires `BillingService` to be `@Autowired` into `AppointmentService`. No schema changes are needed on the `appointments` table.

---

### 3.7 `controller/BillingController.java` — REST API

A `@RestController` (returns JSON, not Thymeleaf views). The controller is intentionally thin — it only parses HTTP input and delegates to `BillingService`.

---

## 4. REST API Reference

### `POST /billing/generate/{appointmentId}`

Triggers invoice generation for a completed appointment.

**Request**
```json
{
  "amount": 1500.00
}
```

**Success Response `200 OK`**
```json
{
  "id": 1,
  "appointmentId": 42,
  "amount": 1500.0,
  "status": "PENDING_PAYMENT"
}
```

**Error Responses**

| HTTP | Cause |
|---|---|
| `400` | `amount` ≤ 0 or missing |
| `400` | A bill already exists for this appointment |

---

### `POST /billing/pay/{billId}`

Processes payment for an existing bill.

**Request**
```json
{
  "paymentMethod": "CARD"
}
```
> Accepted values: `"CARD"` or `"CASH"` (case-insensitive)

**Success Response `200 OK`**
```json
{
  "id": 1,
  "appointmentId": 42,
  "amount": 1500.0,
  "status": "PAID"
}
```

**Error Responses**

| HTTP | Cause |
|---|---|
| `400` | Bill not found |
| `400` | Unknown `paymentMethod` |
| `400` | Bill is not in `PENDING_PAYMENT` state |
| `400` | Payment declined (strategy returns `false`) |

---

### `GET /billing/{billId}`

Retrieves the current state of a bill. Useful for UI polling.

**Success Response `200 OK`**
```json
{
  "id": 1,
  "appointmentId": 42,
  "amount": 1500.0,
  "status": "PROCESSING"
}
```

---

## 5. State Machine — Full Transition Table

```
┌─────────────────────────────────────────────────────────────────┐
│                   BILLING LIFECYCLE (p.7)                       │
│                                                                  │
│  UNBILLED ──[generateInvoice()]──► INVOICE_GENERATED            │
│                                         │                        │
│                         [notifyUser() + markPendingPayment()]    │
│                                         │                        │
│                                         ▼                        │
│                                  PENDING_PAYMENT                 │
│                                         │                        │
│                            [startProcessing()]                   │
│                                         │                        │
│                                         ▼                        │
│                                    PROCESSING                    │
│                                         │                        │
│                        [processPayment(strategy)]                │
│                                         │                        │
│                                         ▼                        │
│                                       PAID                       │
└─────────────────────────────────────────────────────────────────┘
```

Calling any transition method in the wrong order throws `IllegalStateException` — invalid jumps are impossible by design.

---

## 6. Design Principles Applied

| Principle | Where |
|---|---|
| **SRP** — Single Responsibility | `Bill` manages state; `BillingService` orchestrates; `BillingController` handles HTTP |
| **OCP** — Open/Closed | Add a new payment method (e.g., UPI) by creating `UpiPayment implements PaymentStrategy` + one `case` in `resolveStrategy()`. Zero other files change. |
| **DIP** — Dependency Inversion | `Bill.processPayment()` depends on the `PaymentStrategy` interface, never on `CreditCardPayment` or `CashPayment` directly |
| **Strategy Pattern** | `CreditCardPayment` and `CashPayment` are interchangeable at runtime; selected via `resolveStrategy()` in the service layer |

---

## 7. Database Table

Hibernate auto-creates the `bills` table on startup (`spring.jpa.hibernate.ddl-auto=update`). You can inspect it at:

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/healthcare_db
Username: admin
Password: password
```

Expected schema:

```sql
CREATE TABLE bills (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    appointment_id  BIGINT       NOT NULL,
    amount          DOUBLE       NOT NULL,
    status          VARCHAR(255) NOT NULL
);
```

---

## 8. Quick Test with `curl`

```bash
# Step 1: Generate invoice for appointment ID 1, fee ₹1500
curl -X POST http://localhost:8080/billing/generate/1 \
     -H "Content-Type: application/json" \
     -d '{"amount": 1500.00}'

# Step 2: Pay bill ID 1 with card
curl -X POST http://localhost:8080/billing/pay/1 \
     -H "Content-Type: application/json" \
     -d '{"paymentMethod": "CARD"}'

# Step 3: Check bill status
curl http://localhost:8080/billing/1
```
