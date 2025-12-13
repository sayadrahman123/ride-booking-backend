# 🚗 Ride Booking System (Uber-like Backend)

A production-style **Ride Booking Backend System** inspired by Uber, built using **Java & Spring Boot**, demonstrating real-world backend concepts such as **modular, microservice-ready architecture, Redis-based geo-matching, Kafka event streaming, WebSockets, and admin monitoring dashboards**.

This project is designed to showcase **industry-level backend engineering skills**.

---

## 📐 System Design Highlights
- Redis used as a real-time coordination layer (locks + GEO indexing)
- Kafka used for decoupled, asynchronous ride lifecycle events
- WebSockets used only for fan-out delivery (not business logic)
- Stateless REST APIs with JWT-based authentication

---

## 🧠 Key Features

### 🔐 Authentication & Security
- JWT-based authentication
- Role-based access control (USER, DRIVER, ADMIN)
- Secured admin-only APIs

---

### 🚕 Ride Lifecycle Management
- Ride request → accept → start → live tracking → complete
- Driver accept/reject with Redis-based locking
- Ride state persisted in MySQL

---

### 📍 Driver Matching (Redis)
- Redis GEO spatial indexing
- Nearest-driver search using GEOSEARCH
- Atomic driver reservation using Redis locks (SETNX + TTL)

---

### ⚡ Real-Time Updates
- WebSocket (STOMP) for live ride tracking
- Kafka-based event-driven architecture (producer/consumer separation, async ride lifecycle events)
- Events: ride.accepted, ride.started, ride.location.updated, ride.completed

---

### 💰 Fare Calculation & Billing
- Strategy-based, pluggable fare calculation engine (easily extensible)
- Distance + duration-based fare calculation
- Surge multiplier support
- Fare breakdown persisted as JSON



---

### 📊 Admin Dashboard APIs
- View all rides and active rides
- Live driver monitoring (online / busy / available)
- Revenue & metrics dashboard
- Admin-only secured endpoints

---

### 🩺 Monitoring & Stability
- Spring Boot Actuator (health, metrics)
- Global exception handling
- Structured API error responses
- Swagger / OpenAPI documentation

---



## 🛠 Tech Stack

| Category | Technology |
|--------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | MySQL |
| Cache / Geo | Redis |
| Messaging | Apache Kafka |
| Realtime | WebSockets (STOMP) |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA (Hibernate) |
| API Docs | Swagger (OpenAPI) |
| Build Tool | Maven |

---

## 📁 Project Structure

```text
com.example.ridebooking
├── controller
│   ├── admin
│   └── user
├── service
│   |── impl
├── repository
├── entity
├── redis
├── events
├── websocket
├── security
├── exception
└── dto
```
---

## 🚀 Running the Project

### Prerequisites
- Java 17+
- MySQL
- Redis
- Kafka

---

### Steps
```bash
git clone https://github.com/<your-username>/ride-booking-backend.git
cd ride-booking-backend
mvn clean install
mvn spring-boot:run
```

---

## 👤 Author

**Abdus Rahman**  
Java Backend Developer  
> This project was designed and implemented independently as part of a backend system design portfolio.


- GitHub: https://github.com/sayadrahman123  
- LinkedIn: https://www.linkedin.com/in/abdus-sayad-rahman-56471b288/  
- Email: abdusrahman64@gmail.com

