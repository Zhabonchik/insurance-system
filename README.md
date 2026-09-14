# Life Insurance Contract System

A two-service system modelling a simplified life insurance contract issuance workflow. The **insurance-service** is an internal system for an insurance company; the **registry-emulator** simulates a State Insurance Registry where contracts must be registered after issuance.

## Architecture

```
┌─────────────┐       ┌──────────────────────┐       ┌─────────────────────┐
│  Keycloak   │◄─────►│  insurance-service   │──────►│  registry-emulator  │
│  (OAuth2)   │  JWT  │  :8080               │ REST  │  :8081              │
└─────────────┘       └──────────┬───────────┘       └──────────┬──────────┘
                                 │                               │
                          ┌──────▼──────┐                ┌──────▼──────┐
                          │ insurance_db│                │ registry_db │
                          │ (Postgres)  │                │ (Postgres)  │
                          └─────────────┘                └─────────────┘
```

- **insurance-service** — manages applications, contracts, and async registry integration via a transactional outbox.
- **registry-emulator** — emulates an external government registry with switchable failure modes.
- **PostgreSQL 16** — separate databases per service.
- **Keycloak 24** — OAuth2/JWT authentication and role-based authorization.

## Prerequisites

- Docker & Docker Compose

## Quick Start

```bash
docker compose build
docker compose up
```

| Service             | URL                          |
|---------------------|------------------------------|
| insurance-service   | http://localhost:8080        |
| registry-emulator   | http://localhost:8081        |
| Keycloak console    | http://localhost:8082        |
| PostgreSQL          | localhost:5432               |

**Database credentials**: `admin` / `admin_password`  
**Databases**: `insurance_db`, `registry_db`, `keycloak_db`

## Keycloak & Authentication

- **Realm**: `insurance-realm`
- **Admin console**: http://localhost:8082 (admin / admin)

### Roles

| Role       | Permissions                                                    |
|------------|----------------------------------------------------------------|
| `USER`     | Create applications, view own applications and contracts       |
| `EMPLOYEE` | View all applications, approve/reject, issue contracts, view all contracts |

### Test Users

| Username    | Password | Role       |
|-------------|----------|------------|
| `client1`   | `1234`   | `USER`     |
| `employee1` | `1234`   | `EMPLOYEE` |

## REST API

### Insurance Service (`:8080`)

| Method | Endpoint                        | Roles           | Description                    |
|--------|---------------------------------|-----------------|--------------------------------|
| POST   | `/api/applications`             | USER            | Create application             |
| GET    | `/api/applications`             | USER, EMPLOYEE  | List applications (paginated)  |
| GET    | `/api/applications/{id}`        | USER, EMPLOYEE  | Get application by ID          |
| POST   | `/api/applications/{id}/approve`| EMPLOYEE        | Approve application            |
| POST   | `/api/applications/{id}/reject` | EMPLOYEE        | Reject application             |
| POST   | `/api/applications/{id}/contract`| EMPLOYEE       | Issue contract (idempotent)    |
| GET    | `/api/contracts`                | USER, EMPLOYEE  | List contracts (paginated)     |
| GET    | `/api/contracts/{id}`           | USER, EMPLOYEE  | Get contract by ID             |

**Query parameters** for list endpoints: `page`, `size`, `status`, `applicantId` (applications), `contractNumber` (contracts).

### Registry Emulator (`:8081`)

| Method | Endpoint                        | Description                        |
|--------|---------------------------------|------------------------------------|
| POST   | `/api/registry/contracts`       | Register contract (idempotent)     |
| GET    | `/api/registry/contracts`       | List registry records              |
| GET    | `/api/registry/contracts/{id}`  | Get record by external contract ID |
| GET    | `/api/emulator/mode`            | Get current emulator mode          |
| PUT    | `/api/emulator/mode?type=...`   | Switch mode                        |

**Emulator modes**: `SUCCESS`, `BUSINESS_ERROR`, `TECHNICAL_ERROR`, `UNAVAILABLE`

## Postman Collection

A ready-to-use Postman collection and environment are available in the `postman/` directory:

- **Collection**: `postman/Insurance_system.postman_collection.json`
- **Environment**: `postman/Insurance_system.postman_environment.json`

Import both files into Postman via **File → Import**. The collection has token retrieval configured at the collection-level **Authorization** tab, so requests automatically use the correct bearer token once credentials are set in the environment variables.

## Data Model

### Application

| Field          | Type         | Constraints                    |
|----------------|-------------|--------------------------------|
| id             | UUID (PK)   |                                |
| applicant_id   | VARCHAR     | NOT NULL                       |
| applicant_name | VARCHAR     | NOT NULL                       |
| passport_data  | VARCHAR     | NOT NULL                       |
| insured_amount | DECIMAL     | > 0                            |
| term_months    | INTEGER     | > 0                            |
| status         | ENUM        | SUBMITTED, APPROVED, REJECTED  |
| created_at     | TIMESTAMP   | auto                           |

**State machine**: `SUBMITTED` -> `APPROVED` | `REJECTED`

### Contract

| Field           | Type         | Constraints                |
|-----------------|-------------|----------------------------|
| id              | UUID (PK)   |                            |
| application_id  | UUID (FK)   | UNIQUE, NOT NULL           |
| contract_number | VARCHAR     | UNIQUE, NOT NULL           |
| status          | ENUM        | CREATED, REGISTERED        |
| created_at      | TIMESTAMP   | auto                       |

One contract per approved application. Contract number format: `LIFE-{year}-{sequence:06d}`.

### Integration Outbox

| Field          | Type         | Constraints                                       |
|----------------|-------------|---------------------------------------------------|
| id             | UUID (PK)   |                                                   |
| contract_id    | UUID (FK)   | UNIQUE, NOT NULL                                  |
| status         | ENUM        | PENDING, PROCESSING, SUCCESS, FAILED_BUSINESS, DEAD_LETTER |
| retry_count    | INTEGER     |                                                   |
| next_retry_at  | TIMESTAMP   |                                                   |
| last_error     | VARCHAR(500)|                                                   |
| last_attempt_at| TIMESTAMP   |                                                   |
| claimed_at     | TIMESTAMP   |                                                   |

### State Machines

**OutboxStatus**:
```
PENDING ──claim──► PROCESSING ──success──► SUCCESS
                      │
                      ├──permanent fail──► FAILED_BUSINESS
                      │
                      └──transient fail──► PENDING (retry) ──exhausted──► DEAD_LETTER
```

**ContractStatus**:
```
CREATED ──registry confirmed──► REGISTERED
```

## Idempotency & Repeated Operations

### Contract Issuance

- The application row is locked with `SELECT ... FOR UPDATE` before checking for an existing contract.
- If a contract already exists for the application, it is returned without creating a duplicate (HTTP 200 vs 201).
- Concurrent requests are serialized by the row lock; only one contract is ever created per application.

### Registry Registration

- The registry-emulator enforces a `UNIQUE(external_contract_id)` constraint on `registry_record`.
- Concurrent duplicate inserts are handled by catching `DataIntegrityViolationException` and returning the existing record.
- The insurance-service outbox also has `UNIQUE(contract_id)`, preventing duplicate registration attempts at the source.

## Concurrency Handling

- **Application locking**: `FOR UPDATE` on the application row during contract issuance prevents race conditions between concurrent issue requests.
- **Contract number generation**: A single-row sequence table (`contract_number_sequence`) is locked with `FOR UPDATE` to generate unique sequential numbers.
- **Outbox claiming**: `SELECT ... FOR UPDATE SKIP LOCKED` allows multiple scheduler instances to claim outbox rows without contention. Stale `PROCESSING` rows (past lease timeout) are automatically reclaimed.

## Registry Integration — Transactional Outbox

Contract creation and registry registration are **decoupled**:

1. **Issue contract** (synchronous): Creates `Contract` + `IntegrationOutbox(PENDING)` in a single transaction. If the transaction commits, the contract exists and registration is guaranteed to be attempted.
2. **Process outbox** (asynchronous): A scheduler polls for `PENDING` rows every 5 seconds, claims them via `FOR UPDATE SKIP LOCKED`, and dispatches registry calls in virtual threads (bounded concurrency of 5).
3. **Two-phase completion**: After a successful registry call, the outbox row is marked `SUCCESS` and the contract transitions to `REGISTERED`.

This ensures registry unavailability never rolls back contract creation.

## Unavailable Registry Handling

- If the registry is unreachable, the outbox row remains `PENDING` with an incremented `retry_count` and a scheduled `next_retry_at` (exponential backoff).
- The contract stays in `CREATED` status until registration succeeds.
- After the registry recovers, the scheduler automatically picks up pending rows and retries.
- If retries are exhausted (default: 5 attempts), the row moves to `DEAD_LETTER` for manual intervention.
- **Stale lease recovery**: If a worker crashes mid-processing, the `PROCESSING` row is reclaimed after the lease timeout (30s) and treated as a transient failure.

## Error Classification

| Error Type     | HTTP Status         | Retryable | Behavior                          |
|----------------|---------------------|-----------|-----------------------------------|
| Transient      | 500, 503, 408, 429  | Yes       | Exponential backoff, up to 5 retries |
| Permanent      | 400, 404, 409, 422  | No        | Outbox → `FAILED_BUSINESS` immediately |
| Connection     | Timeout, refused    | Yes       | Same as transient                 |

**Backoff formula**: `delay = baseDelay * 2^retryCount`, capped at `maxDelay` (default: 2s, 4s, 8s, 16s, max 5m).

## Emulator Modes

Switch the registry-emulator behavior to test different scenarios:

```bash
# Success (default)
curl -X PUT "http://localhost:8081/api/emulator/mode?type=SUCCESS"

# Business error (422, permanent)
curl -X PUT "http://localhost:8081/api/emulator/mode?type=BUSINESS_ERROR"

# Technical error (500, transient)
curl -X PUT "http://localhost:8081/api/emulator/mode?type=TECHNICAL_ERROR"

# Unavailable (503, transient)
curl -X PUT "http://localhost:8081/api/emulator/mode?type=UNAVAILABLE"
```

## Testing

### Run Tests

```bash
# Unit tests only (no Docker required)
mvn test -pl insurance-service
mvn test -pl registry-emulator

# Integration tests (requires Docker)
# Run from module directory or use failsafe if configured
```

### Test Coverage

| Scenario                              | Unit Test                        | Integration Test                          |
|---------------------------------------|----------------------------------|-------------------------------------------|
| Concurrent contract issuance          | —                                | `ContractIssueServiceIT` (16 threads)     |
| Repeated issuance request             | `ContractIssueServiceTest`       | (covered by concurrent IT)                |
| Repeated registry registration        | `RegistryContractServiceTest`    | `RegistryRegistrationIT`                  |
| Registry unavailability               | `RegistryContractServiceTest`    | `OutboxWorkerIT`, `RegistryRegistrationIT`|
| Recovery after temporary error        | —                                | `OutboxWorkerIT`                          |
| Outbox claim/complete lifecycle       | —                                | `OutboxTwoPhaseIT`                        |
| Application service logic             | `ApplicationServiceTest`         | `ApplicationServiceIT`                    |
| Security (auth/authz)                 | `ApplicationControllerSecurityTest` | —                                    |
| Retry policy                          | `RetryPolicyTest`                | —                                         |
| Scheduler concurrency                 | `OutboxSchedulerTest`            | —                                         |
| Error classification                  | `RegistryErrorClassifierTest`    | —                                         |
| Emulator mode switching               | `EmulatorModeControllerTest`     | —                                         |

## Tech Stack

| Component          | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 21                             |
| Framework          | Spring Boot 4.1.1                   |
| Security           | Spring Security, OAuth2 Resource Server, Keycloak 24 |
| Data               | Spring Data JPA, Hibernate, PostgreSQL 16 |
| Migrations         | Liquibase                           |
| Build              | Maven (multi-module)                |
| Containers         | Docker, Docker Compose              |
| Testing            | JUnit 5, Mockito, AssertJ, Testcontainers |
| Concurrency        | Virtual threads, pessimistic locking |
