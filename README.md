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

- **Java 21** + **Quarkus 3.35.2**
- **PostgreSQL 15** — persistence (bank-api)
- **RabbitMQ 3** — async messaging (bank-api → antifraud-api)
- **Flyway** — database migrations
- **Hibernate ORM with Panache** — persistence layer
- **Hibernate Validator** — request validation (`@Valid`)
- **MicroProfile REST Client** — inter-service HTTP calls
- **Docker / Docker Compose** — container orchestration

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

- **No authentication/authorization.** Every endpoint is currently open — including the internal `PATCH /accounts/{number}/block` endpoint, which should only ever be reachable from `antifraud-api`. JWT-based auth with role-based access control is the next milestone.
- **No idempotency key on payments.** A duplicated request currently processes twice.
- **No rate limiting.**
- **Logs print full account numbers and amounts in plaintext** — should be masked for LGPD/PCI-DSS-style compliance.
- **TED fee debit is a separate call from the main transfer** — not atomic; a partial failure between the two calls is possible.

---

## License

This project is for educational/portfolio purposes.
