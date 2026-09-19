# Verification Record

Verified on 2026-09-19.

| Check | Result |
|---|---|
| Java unit and integration suite | 17 passed, 0 failed |
| Concurrent duplicate transaction ingestion | Passed; one row persisted |
| Bulk ingestion benchmark | 10,000 transactions in 75.82 seconds |
| Required bulk target | Passed; target is under 120 seconds |
| Frontend ESLint | Passed |
| Frontend production build | Passed |
| PostgreSQL runtime | PostgreSQL 17.11 connected successfully |
| Flyway on PostgreSQL | Version 3; all migrations applied |
| PostgreSQL core schema | 7 application tables verified |
| Desktop dashboard | Authenticated queue rendered without overflow |
| Mobile dashboard | 390px viewport rendered without overflow |

## Commands

```sh
mvn test
mvn -Dtest=BulkPerformanceIT test
cd frontend && npm run lint && npm run build
```

The performance result is a local development measurement and should be repeated on deployment-class infrastructure before production sizing.