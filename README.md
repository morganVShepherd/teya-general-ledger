# Teya General Ledger

A Spring Boot-based general ledger system with transaction management, overdraft policies, and message queue processing.

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
│   │   │   ├── Account.java
│   │   │   ├── Transaction.java
│   │   │   ├── OverdraftPolicy.java
│   │   │   ├── MessageQueue.java
│   │   │   ├── TransactionType.java        # DEPOSIT, WITHDRAWAL
│   │   │   ├── TransactionStatus.java      # PENDING, COMPLETED, FAILED
│   │   │   └── EventType.java              # TRANSACTION_COMPLETED, etc.
│   │   ├── repository/                      # Spring Data repositories
│   │   ├── service/                         # Business logic services
│   │   ├── controller/                      # REST API controllers
│   │   ├── dto/                             # Data Transfer Objects
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── exception/                       # Exception handling
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── mapper/                          # MapStruct mappers
│   │   └── util/                            # Utility classes
│   └── resources/
│       ├── application.properties           # Application configuration
│       └── db/
│           └── changelog/
│               ├── db.changelog-master.xml
│               └── v1/
│                   └── db.changelog-001-initial-schema.xml
└── test/
    └── java/moo/interview/teya/
        ├── service/                         # Service unit tests
        ├── controller/                      # Controller tests
        └── integration/                     # Integration tests
```

## Building and Running

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Git

### Build

```bash
cd D:\software\projects\dummy
mvn clean compile
```

### Run

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080` with context path `/api`.

### Test

```bash
mvn test
```

## H2 Console

When running locally, access the H2 database console at:
```
http://localhost:8080/h2-console
```

**Database URL:** `jdbc:h2:mem:testdb`  
**Username:** `sa`  
**Password:** (leave blank)

## API Endpoints

### Account Management
- **Create Account:** `POST /api/accounts`
- **Get Balance:** `GET /api/accounts/{accountNumber}/balance`

### Transactions
- **Deposit:** `POST /api/accounts/{accountNumber}/transactions/deposit`
- **Withdraw:** `POST /api/accounts/{accountNumber}/transactions/withdraw`
- **History:** `GET /api/accounts/{accountNumber}/transactions`

## Key Architecture Decisions

See `ASSUMPTIONS.md` for detailed architectural assumptions and design decisions.

## Database Schema

The system uses 4 main tables:
- **account** - Account information with balance and version for optimistic locking
- **overdraft_policy** - Overdraft settings per account
- **transaction** - Individual transactions with status tracking
- **message_queue** - Event queue for asynchronous processing

## Transaction Processing

Transactions are marked as PENDING when created and only transition to COMPLETED when processed by the message queue job, which runs every 1 second.

## Development Workflow

This project follows Test-Driven Development (TDD):
1. Write failing tests (RED)
2. Implement functionality (GREEN)
3. Refactor code (REFACTOR)

## Git Workflow

```bash
# Initial setup (one time)
git init
git remote add origin https://github.com/morganVShepherd/teya-general-ledger.git

# Regular commits
git add .
git commit -m "Message following conventional commits"
git push -u origin main
```

## License

(Add appropriate license information)

## Contact

Morgan Shepherd

