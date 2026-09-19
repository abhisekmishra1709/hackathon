# Sentinel Architecture

## Request flow

```mermaid
flowchart LR
    CSV[Customer / account / transaction CSV] --> API[Versioned ingestion API]
    Stream[Single transaction REST API] --> API
    API --> Validation[Validation and INR normalization]
    Validation --> DB[(PostgreSQL / H2 dev)]
    Validation --> Rules[Configurable AML engine]
    Rules --> Alerts[Aggregated risk alerts]
    Alerts --> Dashboard[React analyst dashboard]
    Dashboard --> Cases[Case workflow]
    Cases --> Audit[Immutable audit events]
```

## Entity relationships

```mermaid
erDiagram
    CUSTOMER ||--o{ ACCOUNT : owns
    ACCOUNT ||--o{ TRANSACTION : records
    CUSTOMER ||--o{ ALERT : receives
    ACCOUNT ||--o{ ALERT : triggers
    ALERT }o--o{ TRANSACTION : supported_by
    ALERT ||--o| CASE : investigated_as
    CASE ||--o{ AUDIT_EVENT : records

    CUSTOMER {
        bigint id PK
        varchar external_id UK
        varchar full_name
        varchar risk_rating
        boolean politically_exposed
    }
    ACCOUNT {
        bigint id PK
        bigint customer_id FK
        varchar external_id UK
        varchar currency
        decimal current_balance
    }
    TRANSACTION {
        bigint id PK
        bigint account_id FK
        varchar external_id UK
        decimal amount
        decimal amount_inr
        timestamp occurred_at
    }
    ALERT {
        bigint id PK
        bigint customer_id FK
        bigint account_id FK
        varchar dedupe_key UK
        integer risk_score
        varchar status
    }
    CASE {
        bigint id PK
        bigint alert_id FK
        varchar analyst_id
        varchar status
        varchar disposition
    }
    AUDIT_EVENT {
        bigint id PK
        varchar entity_type
        bigint entity_id
        varchar action
        varchar actor_id
    }
```

## Detection model

Every ingested transaction is normalized to INR and evaluated synchronously. Matching rules contribute evidence and a score. The final score is the highest rule score plus five points for each additional match, capped at 100. A unique account/rule/day key aggregates repeated alerts while retaining all evidence.

Rule thresholds and exchange rates are configured under `sentinel.rules` in `application.yml` and can be overridden with environment variables.

## Security and privacy

- `ADMIN`: CSV ingestion, transactions, alerts, cases, and audit events.
- `INGESTOR`: streaming transaction endpoint.
- `ANALYST`: alert and case endpoints.
- List views mask customer names; authenticated alert details reveal the full synthetic identity.
- Closed alerts are retained. Case transitions append audit events and no delete endpoint is provided.