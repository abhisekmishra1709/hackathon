# Sentinel AML: Building Real-Time Money Laundering Detection Platform

## Theme / Summary
Build a Java Spring Boot application that ingests customer, account, and transaction data to detect suspicious patterns indicative of money laundering. The system should apply rule-based and behavioral detection logic, generate risk-scored alerts, and provide case management for compliance analysts.

## Business Scenario
MeridianTrust Bank has grown rapidly over the last three years, onboarding over 500,000 retail and business customers across multiple regions. With growth came scrutiny: the regulator's latest audit flagged the bank's transaction monitoring process as 'manual, reactive, and dangerously slow.' Compliance analysts currently review flagged transactions using spreadsheets exported nightly from the core banking system, often catching suspicious activity 48-72 hours after the fact — well past the window where funds can be frozen or reported to the Financial Intelligence Unit (FIU).

The Chief Compliance Officer has mandated a new Transaction Monitoring System (TMS) codenamed 'Sentinel' that can ingest customer, account, and transaction data, apply configurable detection rules (structuring, rapid fund movement, high-risk jurisdiction transfers, unusual behavioral deviations), and surface prioritized alerts to analysts in near real-time.

Your team has been brought in as an external engineering squad to build a working prototype of Sentinel within the hackathon timeframe. The CCO has one clear expectation: 'I don't need a perfect AI model — I need a system that reliably catches known laundering typologies, explains WHY it flagged something, and gives my analysts a clean workflow to act on it.'

## What to Build
### A) Core Data Model & Ingestion Layer
- Design and implement relational schemas for `Customer`, `Account`, `Transaction`, `Alert`, and `Case` entities with proper relationships (customer → accounts → transactions).
- Build REST/batch ingestion endpoints (or file-upload/CSV import) to load customer KYC data, account metadata (type, currency, opening date, risk rating), and transaction records (amount, currency, counterparty, channel, timestamp, jurisdiction).
- Implement data validation (reject malformed records, enforce referential integrity, log ingestion errors).
- Support incremental/streaming ingestion in addition to bulk load (e.g., a Kafka topic or REST endpoint for new transactions arriving continuously).

### B) Detection Engine
- Implement a rule engine (can be custom or use Drools/similar) that evaluates each transaction/account against configurable AML typologies:
  - **Structuring/Smurfing**: multiple transactions just under reporting thresholds within a time window.
  - **Rapid Movement of Funds**: funds deposited and withdrawn/transferred out within a short window (layering).
  - **High-Risk Jurisdiction Transfers**: transactions to/from sanctioned or high-risk countries.
  - **Unusual Volume/Behavioral Deviation**: transaction volume or value significantly deviating from a customer's historical baseline.
  - **Round-Number / Just-Below-Threshold Patterns**: repeated suspiciously round amounts.
- Each triggered rule must produce an **Alert** with: risk score, triggered rule(s), supporting evidence (transaction IDs), and a human-readable explanation.
- Allow rules to be configured/toggled/tuned (thresholds, time windows) without code redeployment (e.g., via config file, DB table, or admin API).
- Implement alert de-duplication/aggregation so one customer doesn't generate 50 redundant alerts for the same underlying pattern.

### C) (Optional but recommended) Frontend/Dashboard
- Build a simple UI (React/Angular/Vue or server-rendered) showing an alert queue, risk heatmap, customer transaction timeline, and case detail view.

## Business Rules
1. Any single transaction ≥ $10,000 (or currency-adjusted equivalent) must be automatically flagged for review (CTR-style threshold).
2. Three or more transactions from the same account within a 24-hour window that individually fall between $9,000–$9,999 must trigger a 'Structuring' alert.
3. If funds are deposited into an account and ≥80% of that value is transferred out within 48 hours, trigger a 'Rapid Movement' alert.
4. Transactions involving counterparties or jurisdictions on a configurable high-risk/sanctions list must always generate an alert regardless of amount.
5. A customer's daily transaction volume/value that exceeds 3x their 90-day rolling average must trigger a 'Behavioral Deviation' alert.
6. Alerts must never be silently deleted — cleared alerts remain in the system with disposition reason and analyst identity for audit purposes.
7. Each alert must carry a calculated risk score (e.g., 0-100) derived from a weighted combination of triggered rules; higher-risk alerts must be sortable to the top of the analyst queue.
8. Sensitive PII (customer name, ID numbers) must be masked in list views and only fully visible in detail views to authorized roles.
9. All monetary amounts must be normalized to a base currency (e.g., INR) for cross-transaction comparison, using a configurable exchange rate table.

## Constraints & Non-Functional Requirements
- **Technology mandate**: Backend must be built in Java 17+ using Spring Boot (Spring Data JPA, Spring Security, Spring Web/WebFlux). Build tool: Maven or Gradle.
- **Database**: Use a relational database (PostgreSQL preferred; Oracle or MySQL acceptable) with a documented schema/ERD and migration scripts.
- **Performance**: The detection engine should be able to process at least 10,000 simulated transactions within a reasonable time (target: under 2 minutes) for a bulk load scenario, and evaluate individual streaming transactions within sub-second latency where feasible.
- **Concurrency**: Ingestion and detection processing must be thread-safe and support concurrent transaction streams without duplicate/lost alerts.
- **Security**: Passwords/secrets must not be hardcoded; use environment variables or a secrets manager. Role-based access control must be enforced at the API layer, not just UI. All API endpoints must use proper HTTP status codes and input validation.
- **API standards**: RESTful design with versioned endpoints (e.g., `/api/v1/...`), consistent JSON error responses, and OpenAPI/Swagger documentation.
- **Auditability**: All alert/case state transitions must be logged immutably with timestamp and actor identity.
- **Code quality**: Clean layered architecture (controller/service/repository), unit tests for detection rules (JUnit + Mockito), and meaningful logging (SLF4J).
- **Data privacy**: No real PII should be used; provide realistic synthetic seed data for customers/accounts/transactions.

## Extension Ideas
- Add a machine-learning based anomaly scoring model (e.g., isolation forest or clustering) to complement rule-based detection and reduce false positives.
- Build a network/graph visualization showing linked accounts and transaction flows to detect layering rings or shell company networks.
- Implement a real-time streaming pipeline using Kafka + Spring Cloud Stream for continuous transaction ingestion and sub-second alerting.
- Add automated SAR (Suspicious Activity Report) draft generation summarizing evidence in narrative form for regulatory filing.
- Build an analyst productivity dashboard with metrics (alert volume trends, false-positive rate, average time-to-disposition) using charts.
- Implement configurable rule versioning so compliance teams can A/B test rule threshold changes and measure impact on alert volume/quality.

## Technical Requirements
- **Primary Database:** postgres
- **Preferred API Language:** java
- **Preferred Frontend Frameworks:** angular, any, react

## Focus Areas
- Backend architecture (Java/Spring Boot)
- Rule engine / detection logic
- Data modeling for financial domain
- Case management workflow
- Security & audit compliance
- API design

## Required Deliverables
- Working Spring Boot application with source code in a Git repository
- Database schema/ERD with migration scripts
- Seed/synthetic dataset for customers
- accounts
- and transactions covering at least 3 laundering typologies
- Documented REST API (OpenAPI/Swagger spec)
- Unit tests covering detection rule logic
- README explaining architecture
- setup instructions
- and rule configuration approach
- Demo walkthrough (video or live) showing ingestion → detection → alert → case disposition flow