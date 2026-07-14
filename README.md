# Bank Ecosystem

A microservices-based banking simulation built with **Quarkus** (Java), demonstrating a realistic payment processing pipeline with asynchronous fraud detection.

> Built as a portfolio project to practice distributed systems design, secure API development, and container orchestration.

---

## Architecture

Three independent services communicate over REST and asynchronous messaging (RabbitMQ), backed by a shared PostgreSQL database owned by `bank-api`.

```
                    ┌──────────────┐
   HTTP Request     │              │
  ─────────────────▶│  payment-api │
   (PIX/TED/Credit/  │   :8082     │
    Boleto)          └──────┬───────┘
                             │ REST
                             ▼
                     ┌──────────────┐        fanout exchange
                     │   bank-api   │  ─────────────────────────┐
                     │    :8080     │  "suspicious_transfers"   │
                     └──────┬───────┘                           │
                             │                                  ▼
                             │ JDBC                     ┌──────────────┐
                             ▼                           │ antifraud-api│
                     ┌──────────────┐                    │    :8081     │
                     │  PostgreSQL  │                    └──────┬───────┘
                     └──────────────┘                           │
                                                                  │ REST
                             ▲                                  │ (block account)
                             └──────────────────────────────────┘
```

### Services

| Service | Port | Responsibility |
|---|---|---|
| **bank-api** | 8080 | Core ledger: accounts, balances, deposits, withdrawals, transfers. Publishes an event to RabbitMQ whenever a transfer exceeds a suspicious threshold. |
| **payment-api** | 8082 | Payment gateway. Implements the Strategy pattern for PIX, TED, Credit Card, and Boleto — each with its own fee and routing logic. Delegates actual money movement to `bank-api`. |
| **antifraud-api** | 8081 | Asynchronous fraud analyzer. Consumes suspicious transfer events from RabbitMQ; if the amount exceeds a hard threshold, calls `bank-api` to block both accounts involved. |

### Why this design

- **Strategy Pattern** (`payment-api`) — each payment method (PIX, TED, Credit Card, Boleto) has distinct fee rules and settlement flow, encapsulated in its own class implementing a common `PaymentStrategy` interface.
- **Async fraud detection** — flagging a transfer as suspicious shouldn't block the transaction itself. `bank-api` fires a fanout event and moves on; `antifraud-api` reacts independently.
- **Fanout exchange** — the fraud alert has exactly one purpose (notify the fraud analyzer), no routing logic needed. `fanout` broadcasts to every bound queue regardless of routing key, avoiding a whole class of silent-delivery-failure bugs that `topic`/`direct` exchanges are prone to when publisher and consumer configs drift apart.
- **Multi-module Maven mono-repo** — all three services share one parent `pom.xml`, keeping dependency and Java versions consistent, while still being independently buildable and deployable.

---

## Tech Stack

- **Java 21** + **Quarkus 3.37.2**
- **PostgreSQL 15** — persistence (bank-api)
- **RabbitMQ 3** — async messaging (bank-api → antifraud-api)
- **Flyway** — database migrations
- **Hibernate ORM with Panache** — persistence layer
- **Hibernate Validator** — request validation (`@Valid`)
- **MicroProfile REST Client** — inter-service HTTP calls
- **Docker / Docker Compose** — container orchestration

---

## Security & Resilience

This ecosystem incorporates industry-standard application security (AppSec) patterns, specifically targeted at a cybersecurity-focused portfolio:

### 1. Authentication & Authorization
- **JWT & Role-Based Access Control (RBAC)**: All user endpoints are secured via JWT authentication. Role scopes (e.g., `USER`, `ADMIN`) are enforced natively using MicroProfile JWT.
- **Service-to-Service Security**: Sensitive internal actions (such as account blocking) are protected via a shared API key checked by [InternalAuthFilter.java](file:///home/leonardogm/Projetos/BankEcosystem/bank-api/src/main/java/br/com/bankApi/security/InternalAuthFilter.java).

### 2. Rate Limiting & Lockout
- **API Rate Limiting**: An in-memory sliding-window IP rate limiter is implemented at [RateLimitFilter.java](file:///home/leonardogm/Projetos/BankEcosystem/bank-api/src/main/java/br/com/bankApi/security/RateLimitFilter.java), applying granular limits depending on endpoint sensitivity.
- **Brute-Force Protection**: Credentials include password hashing and automatic account lockout windows to prevent brute-force attacks.

### 3. Supply Chain Security (Vulnerability Scanning)
- **OWASP Dependency-Check**: Configured reactor-wide in the parent `pom.xml` to scan all libraries for known CVEs.
- **Active Patching**: Core dependencies such as `jackson-databind` and `postgresql` are actively overridden to non-vulnerable versions (`2.22.1` and `42.7.13`).
- **Suppression Management**: False positives (e.g., matching Java libraries to Go CVEs or name-based misidentifications) are formally documented and managed in [dependency-check-suppressions.xml](file:///home/leonardogm/Projetos/BankEcosystem/dependency-check-suppressions.xml).

### 4. Hardened HTTP Security Headers & CORS
- **Default Headers**: Response headers (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`, and `Strict-Transport-Security`) are enforced globally.
- **Explicit CORS Decision**: CORS is explicitly disabled because this is a backend-to-backend REST API with no web frontend. Disabling CORS prevents browser-based cross-origin script abuse entirely.

---

## Running the project

### Prerequisites

- Docker and Docker Compose
- (Optional, for local dev outside Docker) Java 21 and Maven

### 1. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` and set real values for `DB_PASSWORD` and `RABBIT_PASSWORD` at minimum.

### 2. Start everything

```bash
docker-compose up --build
```

This builds and starts 5 containers: `postgres`, `rabbitmq`, `bank-api`, `payment-api`, `antifraud-api`. Subsequent runs (no code changes) can drop `--build`.

| Service | URL |
|---|---|
| bank-api | http://localhost:8080 |
| payment-api | http://localhost:8082 |
| antifraud-api | http://localhost:8081 |
| RabbitMQ Management UI | http://localhost:15672 |

### 3. Stop everything

```bash
docker-compose down
```

Data persists in a named Docker volume — accounts and transactions survive a restart. To wipe the database too, add `-v`.

### Local development (hot reload)

Each service can also run standalone with Quarkus dev mode, without Docker:

```bash
cd bank-api && mvn quarkus:dev
```

Defaults are pre-configured in each `application.properties`, so this works without any `.env` file — Docker and local dev are independent paths.

---

## Example: making a PIX transfer

```http
POST http://localhost:8082/payments
Content-Type: application/json
 
{
  "amount": 1500.00,
  "method": "PIX",
  "destinationAccountNumber": 509064,
  "sourceAccountNumber": 462293
}
```

Transfers above the configured threshold automatically trigger the fraud analysis pipeline — `bank-api` publishes an event, `antifraud-api` consumes it, and blocks both accounts if the amount exceeds the hard limit.

---

## Project structure

```
BankEcosystem/
├── pom.xml                  # parent Maven module
├── docker-compose.yml
├── .env.example
├── bank-api/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/br/com/bankApi/
│       ├── account/         # Account entity, service, REST resource
│       ├── client/          # Client entity, service, REST resource
│       ├── transaction/     # Transfer/deposit/withdraw/refund logic
│       └── exception/       # Global exception handler
├── payment-api/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/br/com/PaymentAPI/
│       ├── strategy/        # Pix/Ted/Credit/BankSlip strategies
│       ├── client/          # REST client to bank-api
│       ├── dto/
│       └── exception/
└── antifraud-api/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/br/com/AntiFraudAPI/
        ├── client/           # REST client to bank-api
        └── FraudAnalyzerService.java
```

---

## Known limitations / next steps

This is an evolving portfolio project. Current gaps, in rough priority order:

- **No idempotency key on payments**: A duplicated payment request currently processes twice. Real-world financial systems require idempotency keys to handle network retries safely.
- **Log masking for accounts and amounts**: Only CPF is currently masked in logging. To achieve PCI-DSS or LGPD compliance, full account numbers and transaction amounts should also be masked.
- **Non-atomic TED fee debit**: Processing fee debit is currently a separate call from the main transfer logic, creating a race condition where one can fail and the other succeeds.
- **Distributed tracing**: For production monitoring, distributed tracing (e.g., OpenTelemetry) should be fully connected across the async RabbitMQ boundary.

---

## License

This project is for educational/portfolio purposes.
