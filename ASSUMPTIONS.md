# Teya General Ledger - Architecture Assumptions

**Date:** July 12, 2026  
**Version:** 1.0

## Overview

This document outlines all architectural assumptions, design decisions, and constraints for the Teya General Ledger system.

## Database Schema

### 1. Account Table
```sql
CREATE TABLE account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_number VARCHAR(8) UNIQUE NOT NULL,
    current_balance DECIMAL(19,6) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'GBP',
    version BIGINT NOT NULL DEFAULT 0,
    created_at_in_utc TIMESTAMP NOT NULL,
    updated_at_in_utc TIMESTAMP NOT NULL
);
```

**Key Decisions:**
- Account numbers are auto-generated in format: ACC-00000001, ACC-00000002, etc. (8 digits, zero-padded)
- Balance stored with 6 decimal places (DECIMAL(19,6)) for precision
- Default currency is GBP
- `version` field enables optimistic locking
- All timestamps in UTC

### 2. Overdraft Policy Table
```sql
CREATE TABLE overdraft_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT UNIQUE NOT NULL,
    overdraft_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    overdraft_limit DECIMAL(19,6),
    created_at_in_utc TIMESTAMP NOT NULL,
    updated_at_in_utc TIMESTAMP NOT NULL,
    FOREIGN KEY (account_id) REFERENCES account(id)
);
```

**Key Decisions:**
- One-to-one relationship with Account
- Overdraft is disabled by default
- Limit is optional and only applies if overdraft_allowed is true

### 3. Transaction Table
```sql
CREATE TABLE transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,6) NOT NULL,
    balance_after DECIMAL(19,6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    created_at_in_utc TIMESTAMP NOT NULL,
    FOREIGN KEY (account_id) REFERENCES account(id)
);
```

**Key Decisions:**
- Transactions are created with status PENDING
- Only transition to COMPLETED via message queue processor
- `balance_after` records the balance immediately after the transaction
- Cannot cancel transactions (immutable once created)
- Status lifecycle: PENDING → COMPLETED or PENDING → FAILED

### 4. Message Queue Table
```sql
CREATE TABLE message_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_number VARCHAR(8) NOT NULL,
    transaction_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INT NOT NULL DEFAULT 0,
    created_at_in_utc TIMESTAMP NOT NULL,
    processed_at_in_utc TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transaction(id)
);
```

**Key Decisions:**
- Event-based architecture for asynchronous processing
- Messages support up to 3 retries for failed withdrawals
- Retry logic waits 1 second between attempts
- Synchronization prevents duplicate processing

## Service Layer

### Transaction Processing

**Key Principle:** All transactions are created PENDING and only transition to COMPLETED when processed by the scheduled message queue job.

#### Deposit Flow
1. Create transaction with PENDING status
2. Publish event to message queue
3. Message queue processor immediately processes (usually within 1 second)
4. Transaction marked COMPLETED
5. Account balance updated

#### Withdrawal Flow
1. Validate account exists
2. Check balance availability (accounting for pending transactions)
3. Create transaction with PENDING status
4. Publish event to message queue
5. Message queue processor attempts to process
6. If prior pending transaction exists, retry logic:
   - Waits 1 second
   - Checks if prior transaction completed
   - Retries up to 3 times total
7. Transaction marked COMPLETED or FAILED
8. Account balance updated (if COMPLETED)

### Retry Logic

**Withdrawal Retry Behavior:**
- Only applies to PENDING prior transactions (sequential ordering)
- Does NOT wait for future transactions
- Retries 3 times maximum with 1-second pauses
- If prior transaction still PENDING after 3 retries, withdrawal FAILS
- Ensures sequential consistency of transaction processing

### Optimistic Locking

**Implementation:**
- Account entity uses JPA `@Version` field
- Automatically incremented on each update
- Prevents concurrent modification conflicts
- Throws `OptimisticLockException` on conflict
- Returns HTTP 409 Conflict

### Message Queue Processor

**Scheduler Configuration:**
- Runs every 1 second (configurable)
- Cron expression: `0 * * * * ?`
- Synchronized to prevent duplicate processing
- Single-threaded execution

**Processing Rules:**
1. Query unprocessed messages ordered by creation time
2. For each message:
   - Process in order (maintains FIFO for same account)
   - Update transaction status to COMPLETED
   - Update account balance
   - Mark message as processed
   - Increment retry_count if applicable
3. Commit transaction for each processed message

## API Layer

### Account Creation
- **Endpoint:** `POST /api/accounts`
- **Request:** Empty body `{}`
- **Response:** 201 Created with new account details
- **Auto-generation:** Account numbers always generated, user cannot specify

### Deposit
- **Endpoint:** `POST /api/accounts/{accountNumber}/transactions/deposit`
- **Request:** `{ "amount": "123.45", "description": "Optional" }`
- **Response:** 201 Created (synchronous - transaction already COMPLETED)
- **Status Codes:**
  - 201 Created - Success
  - 400 Bad Request - Invalid amount or validation failure
  - 404 Not Found - Account not found

### Withdrawal
- **Endpoint:** `POST /api/accounts/{accountNumber}/transactions/withdraw`
- **Request:** `{ "amount": "123.45", "description": "Optional" }`
- **Response:** 202 Accepted (asynchronous - transaction still PENDING)
- **Status Codes:**
  - 202 Accepted - Withdrawal submitted
  - 400 Bad Request - Invalid amount or validation failure
  - 404 Not Found - Account not found
  - 409 Conflict - Insufficient balance or overdraft violation

### Get Balance
- **Endpoint:** `GET /api/accounts/{accountNumber}/balance`
- **Response:** 200 OK with balance and lastUpdatedAtInUTC
- **Format:**
  ```json
  {
    "accountNumber": "ACC-00000001",
    "balance": "123.45",
    "currency": "GBP",
    "lastUpdatedAtInUTC": "2026-07-12T10:30:45Z"
  }
  ```

### Transaction History
- **Endpoint:** `GET /api/accounts/{accountNumber}/transactions?pageSize=20&cursor=abc123&fromDate=...&toDate=...`
- **Response:** 200 OK with paginated results and next cursor
- **Pagination:** Cursor-based (opaque, encoded)
- **Filters:** Optional date range (fromDate, toDate)
- **Constraints:**
  - pageSize: 1-100 (default 20)
  - fromDate must be ≤ toDate
  - Dates in ISO-8601 format (UTC)

## Currency and Precision

### Storage
- **Precision:** 6 decimal places (DECIMAL(19,6))
- **Rounding:** None at storage level
- **Rationale:** Maintains financial precision for GBP (typically 2 decimals)

### Display
- **Precision:** 2 decimal places
- **Rounding:** DOWN (floor function)
- **Rationale:** Display 123.456 as 123.45, not 123.46
- **Serializer:** `MoneySerializer` (Jackson)

### Multi-Currency
- **Current Support:** Single currency per account (GBP default)
- **Deposit/Withdraw:** Must use account's currency
- **No Conversion:** System does NOT support currency conversion
- **Future Enhancement:** Multi-currency support requires schema changes

## Timestamps

### Format
- **Standard:** ISO-8601 with UTC timezone
- **Example:** `2026-07-12T10:30:45.123Z` (with milliseconds)
- **Java Type:** `java.time.Instant` (preferred) or `OffsetDateTime`

### Variable Naming Convention
- Creation: `createdAtInUTC`
- Update: `updatedAtInUTC`
- Processing: `processedAtInUTC`
- Query: `lastUpdatedAtInUTC`

### Database Storage
- H2: TIMESTAMP type (implicitly UTC)
- Liquibase: TIMESTAMP WITHOUT TIME ZONE
- JPA: `@Temporal(TemporalType.TIMESTAMP)` or `java.time.Instant`

## Cursor Pagination

### Cursor Encoding
- **Algorithm:** Simple ID reversal (not cryptographic)
- **Example:** ID 12345 → cursor "54321"
- **Rationale:** Obfuscation only, not security
- **Service:** `CursorCryptoService`

### Cursor Usage
1. Client requests first page: `GET /transactions` (no cursor)
2. Response includes `nextCursor: "54321"`
3. Client requests next page: `GET /transactions?cursor=54321`
4. Response includes next cursor or null if end of data

## Error Handling

### Error Response Format
```json
{
  "errorCode": "VALIDATION_ERROR",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-07-12T10:30:45Z",
  "message": "Amount must be greater than 0"
}
```

### Error Codes
| Code | HTTP Status | Meaning |
|------|-------------|---------|
| VALIDATION_ERROR | 400 | Invalid input parameters |
| NOT_FOUND | 404 | Account or transaction not found |
| INSUFFICIENT_BALANCE | 409 | Not enough balance for withdrawal |
| PENDING_TRANSACTION_TIMEOUT | 409 | Prior transaction still pending after retries |
| OVERDRAFT_VIOLATION | 409 | Withdrawal exceeds overdraft limit |
| OPTIMISTIC_LOCK_FAILURE | 409 | Concurrent modification detected |
| INTERNAL_ERROR | 500 | Unexpected server error |

### Exception Hierarchy
- `IllegalArgumentException` - Validation errors
- `EntityNotFoundException` - Not found errors
- `OptimisticLockException` - Concurrent modification

## Testing Strategy

### Unit Tests
- **Framework:** JUnit 5
- **Mocking:** Mockito
- **Scope:** Services, utilities, mappers
- **Approach:** Mock repositories and external dependencies

### Integration Tests
- **Framework:** JUnit 5 + @SpringBootTest
- **Database:** Real H2 in-memory database
- **REST Client:** MockMvc or TestRestTemplate
- **Approach:** Test complete workflow through REST API

### TDD Approach
1. Write failing test (RED)
2. Implement minimal code to pass (GREEN)
3. Refactor without changing behavior (REFACTOR)
4. Repeat for each feature

### Test Data
- Fixtures in test setup
- H2 allows fresh database per test
- No test data pollution between tests

## Concurrency Considerations

### Thread Safety
- Message queue processor synchronized (single-threaded)
- Account balance updates via optimistic locking
- Spring Data JPA handles transaction boundaries

### Race Conditions Addressed
1. **Concurrent Deposits:** Both succeed (balance = sum)
2. **Concurrent Withdrawals:** Optimistic lock detects conflict
3. **Concurrent Deposit + Withdrawal:** Lock prevents race
4. **Duplicate Message Processing:** Synchronization prevents
5. **Retry Logic Race:** Sequential processing prevents

## Validation Rules

### Deposit Validation
- ✓ Amount > 0
- ✓ Amount ≤ 9999999.99
- ✓ Account exists
- ✓ Currency matches

### Withdrawal Validation
- ✓ Amount > 0
- ✓ Amount ≤ 9999999.99
- ✓ Account exists
- ✓ Currency matches
- ✓ Balance sufficient (including pending transactions)
- ✓ Overdraft policy respected (if applicable)

### Account Creation Validation
- ✓ Request body must be empty `{}`
- ✓ Always generates new account
- ✓ No duplicate account numbers

## Deployment Considerations

### Configuration Management
- Environment-specific properties via `application.properties`
- Liquibase migrations auto-run on startup
- H2 database resets on restart (in-memory)

### Production Notes
- Replace H2 with PostgreSQL
- Update `spring.jpa.hibernate.ddl-auto` to `validate`
- Configure proper database backup strategy
- Implement API rate limiting
- Add authentication/authorization
- Monitor scheduled job execution

## Future Enhancements

1. Multi-currency support with exchange rates
2. API authentication (OAuth2/JWT)
3. Audit logging
4. Idempotency keys for duplicate prevention
5. Batch transaction processing
6. Analytics and reporting endpoints
7. Webhook notifications for transaction completion

## Assumptions Summary

| Assumption | Status |
|-----------|--------|
| Java 17+ available | ✓ Confirmed |
| Spring Boot 3.x | ✓ Confirmed |
| H2 database sufficient | ✓ Confirmed |
| Single currency (GBP) | ✓ Confirmed |
| Optimistic locking adequate | ✓ Confirmed |
| 1-second job frequency acceptable | ✓ Confirmed |
| 3-retry limit for withdrawals | ✓ Confirmed |
| Cursor-based pagination preferred | ✓ Confirmed |

---

**Last Updated:** July 12, 2026

