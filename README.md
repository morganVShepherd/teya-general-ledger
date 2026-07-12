# Teya General Ledger

A Spring Boot-based general ledger system with transaction management, overdraft policies, and message queue processing.

---

## Starting the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### 1. Build

```bash
mvn clean compile
```

### 2. Run

```bash
mvn spring-boot:run
```

The application starts on **`http://localhost:8080`** with context path **`/api`**.

> **Note:** The database is in-memory (H2). All data — including the seed accounts below — is available immediately on startup and is reset each time the app restarts.

### 3. Run Tests

```bash
mvn test
```

---

## API Walkthrough (using seed data)

The following accounts are pre-loaded by Liquibase on startup:

| Account Number | Starting Balance | Overdraft Allowed | Overdraft Limit |
|----------------|-----------------|-------------------|-----------------|
| `INI-001`      | £10.00          | No                | —               |
| `INI-002`      | £50.00          | No                | —               |
| `INI-003`      | £100.00         | Yes               | £500.00         |
| `INI-004`      | £500.00         | Yes               | £1,000.00       |

Use any HTTP client (Postman, curl, HTTPie, etc.) with `Content-Type: application/json`.

For every **deposit** and **withdrawal** request, include a `currency` field in the JSON body. It **must match** the currency stored on the account. If it does not, the transaction is immediately marked as `FAILED` and the API returns a `409 Conflict` with error code `CURRENCY_MISMATCH`.

---

### Check an Account Balance

```
GET http://localhost:8080/api/v1/accounts/INI-003/balance
```

**Expected response (`200 OK`):**
```json
{
  "accountNumber": "INI-003",
  "currentBalance": "100.00",
  "currency": "GBP",
  "lastUpdatedAtInUTC": "..."
}
```

---

### Make a Deposit

```
POST http://localhost:8080/api/v1/accounts/INI-003/transactions/deposit
```
```json
{
  "amount": 50.00,
  "currency": "GBP",
  "description": "Top up"
}
```

**Expected response (`201 Created`):**
```json
{
  "amount": "50.00",
  "balanceAfter": "150.00",
  "transactionType": "DEPOSIT",
  "status": "PENDING",
  "currency": "GBP",
  "description": "Top up"
}
```

> Withdrawals are processed asynchronously. The status transitions from `PENDING` → `COMPLETED` within ~1 second.

---

### Example 1 — Overdraft Allowed (`INI-003`)

Account `INI-003` starts with **£100.00** and has an overdraft limit of **£500.00**, meaning the balance can go as low as **−£500.00**.

Withdraw **£450.00** (£350.00 beyond the current balance, but within the overdraft limit):

```
POST http://localhost:8080/api/v1/accounts/INI-003/transactions/withdraw
```
```json
{
  "amount": 450.00,
  "currency": "GBP",
  "description": "Large withdrawal using overdraft"
}
```

**Expected response (`202 Accepted`):**
```json
{
  "amount": "450.00",
  "balanceAfter": "-350.00",
  "transactionType": "WITHDRAWAL",
  "status": "PENDING",
  "currency": "GBP",
  "description": "Large withdrawal using overdraft"
}
```

Poll the balance to confirm it settles at **−£350.00**:

```
GET http://localhost:8080/api/v1/accounts/INI-003/balance
```

**Expected response (after ~1 second):**
```json
{
  "accountNumber": "INI-003",
  "currentBalance": "-350.00",
  "currency": "GBP",
  "lastUpdatedAtInUTC": "..."
}
```

---

### Example 2 — Overdraft Denied (`INI-001`)

Account `INI-001` starts with **£10.00** and has **no overdraft**. Any withdrawal exceeding the balance is rejected immediately.

Attempt to withdraw **£50.00** (exceeds the £10.00 balance):

```
POST http://localhost:8080/api/v1/accounts/INI-001/transactions/withdraw
```
```json
{
  "amount": 50.00,
  "currency": "GBP",
  "description": "Withdrawal that exceeds balance"
}
```

**Expected response (`409 Conflict`):**
```json
{
  "errorCode": "INSUFFICIENT_BALANCE",
  "message": "Insufficient balance for this transaction"
}
```

The account balance remains unchanged at **£10.00**.

---

### Example 3 — Currency Mismatch (`INI-003`)

Account `INI-003` is stored in **GBP**. If you send a deposit or withdrawal request in another currency, the request fails immediately.

```
POST http://localhost:8080/api/v1/accounts/INI-003/transactions/deposit
```
```json
{
  "amount": 50.00,
  "currency": "EUR",
  "description": "Wrong currency"
}
```

**Expected response (`409 Conflict`):**
```json
{
  "errorCode": "CURRENCY_MISMATCH",
  "message": "Transaction currency must match account currency"
}
```

The account balance remains unchanged, and the failed attempt is recorded in transaction history with status `FAILED`.

---

### View Transaction History

```
GET http://localhost:8080/api/v1/accounts/INI-003/transactions?pageSize=10
```

**Expected response (`200 OK`):**
```json
{
  "transactions": [
    {
      "transactionType": "WITHDRAWAL",
      "amount": "450.00",
      "balanceAfter": "-350.00",
      "status": "COMPLETED",
      "currency": "GBP",
      "description": "Large withdrawal using overdraft"
    }
  ],
  "pageInfo": {
    "hasNextPage": false,
    "nextCursor": null
  }
}
```

#### Cursor-based pagination

```
GET http://localhost:8080/api/v1/accounts/INI-003/transactions?pageSize=2
```

Use the `nextCursor` value from `pageInfo` to fetch the next page:

```
GET http://localhost:8080/api/v1/accounts/INI-003/transactions?pageSize=2&cursor=<nextCursor>
```

---

## H2 Console

Inspect the live database at:

```
http://localhost:8080/api/h2-console
```

| Setting      | Value               |
|--------------|---------------------|
| JDBC URL     | `jdbc:h2:mem:testdb`|
| Username     | `sa`                |
| Password     | *(leave blank)*     |

---

## Overview

The Teya General Ledger is a financial transaction system that manages accounts, deposits, withdrawals, and maintains a transaction history. It supports overdraft policies and uses a message queue system for asynchronous transaction processing.

## Technical Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (in-memory)
- **Liquibase** (database migrations)
- **Lombok** (code generation)
- **MapStruct** (object mapping)
- **JUnit 5** (testing)
- **Mockito** (mocking)

## Project Structure

```
src/
├── main/
│   ├── java/moo/interview/teya/
│   │   ├── Application.java                 # Spring Boot application entry point
│   │   ├── config/                          # Configuration classes
│   │   │   ├── SchedulerConfig.java        # Scheduling configuration
│   │   │   └── JsonConfig.java             # JSON serialization config
│   │   ├── entity/                          # JPA entity classes and enums
│   │   ├── repository/                      # Spring Data repositories
│   │   ├── service/                         # Business logic services
│   │   ├── controller/                      # REST API controllers
│   │   ├── dto/                             # Data Transfer Objects
│   │   ├── exception/                       # Exception handling
│   │   ├── mapper/                          # MapStruct mappers
│   │   └── util/                            # Utility classes
│   └── resources/
│       ├── application.properties
│       └── db/changelog/
└── test/
    └── java/moo/interview/teya/
        ├── service/
        ├── controller/
        └── integration/
```

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/api/v1/accounts` | Create a new account |
| `GET`  | `/api/v1/accounts/{accountNumber}/balance` | Get current balance |
| `POST` | `/api/v1/accounts/{accountNumber}/transactions/deposit` | Deposit funds |
| `POST` | `/api/v1/accounts/{accountNumber}/transactions/withdraw` | Withdraw funds |
| `GET`  | `/api/v1/accounts/{accountNumber}/transactions` | Transaction history (paginated) |

## Key Architecture Decisions

See `ASSUMPTIONS.md` for detailed architectural assumptions and design decisions.

## Transaction Processing

Transactions are marked as `PENDING` when created and transition to `COMPLETED` when processed by the message queue job, which runs every 1 second.

## Note about optimistic locking (`version` column)

The initial Liquibase schema includes a `version` column on the `account` table. During development we removed the `@Version` field from the `Account` entity to leave the optimistic-locking strategy as a design decision to discuss during the interview. The `version` column remains in the DB migrations so you can demonstrate or experiment with it if desired.

## Contact

Morgan Shepherd
