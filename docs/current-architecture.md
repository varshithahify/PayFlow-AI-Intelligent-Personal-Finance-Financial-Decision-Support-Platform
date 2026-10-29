# PayFlow AI — Current Architecture

## 1. Overview

PayFlow AI is a full-stack payment processing and reconciliation platform designed to detect inconsistencies between internal transaction records and external payment records.

The platform provides transaction management, payment gateway integration, asynchronous event processing, reconciliation, investigation management, and a web-based interface.

The planned AI extension will help users understand payment discrepancies by analysing available evidence, generating explanations, and recommending investigation steps.

AI recommendations will remain advisory. The AI will not independently modify financial records, initiate payments, or resolve investigations.

## 2. Technology Stack

| Component                 | Technology                               |
| ------------------------- | ---------------------------------------- |
| Programming language      | Java 21                                  |
| Backend framework         | Spring Boot                              |
| API architecture          | REST APIs                                |
| Database                  | PostgreSQL                               |
| ORM                       | JPA and Hibernate                        |
| Database migrations       | Flyway                                   |
| Authentication            | Spring Security and JWT                  |
| Messaging                 | Apache Kafka                             |
| Caching                   | Redis                                    |
| Frontend                  | React                                    |
| Frontend state management | Redux                                    |
| Build tool                | Maven                                    |
| API testing               | Postman and automated tests              |
| AI integration            | Spring AI and a model provider — planned |

## 3. High-Level Architecture

The system consists of the following layers.

### Frontend Layer

React and Redux provide the user interface for transactions, reconciliation results, investigations, and operational monitoring.

### API and Security Layer

Spring Boot controllers expose REST endpoints. Spring Security and JWT provide authentication, while tenant-aware authorization must ensure users can access only permitted organizational data.

### Business Logic Layer

Services implement transaction processing, reconciliation, investigation management, settlement processing, webhook delivery, and event recovery.

### Persistence Layer

PostgreSQL stores transaction, external payment, reconciliation, investigation, user, and event-processing records. JPA and Hibernate map Java entities to database tables. Flyway manages schema migrations.

### Messaging and Caching Layer

Kafka supports asynchronous event processing. Redis provides transaction caching through the existing cache service.

### External Integration Layer

Payment gateway abstractions support gateway routing. The application also contains a fraud-service client and an external-payment simulator.

### Planned AI Layer

A dedicated AI investigation service will collect authorized evidence, request an explanation from the configured model provider, validate the response, and return structured recommendations to the frontend.

## 4. Backend Package Structure

Base package: `com.payflow.payflow_backend`

| Package      | Responsibility                                                                                                             |
| ------------ | -------------------------------------------------------------------------------------------------------------------------- |
| `client`     | External service clients, including fraud-service integration                                                              |
| `config`     | Application, Kafka, REST client, password, JSON, and security configuration                                                |
| `controller` | REST endpoints for authentication, payments, transactions, reconciliation, investigations, users, simulation, and webhooks |
| `dto`        | API request and response objects                                                                                           |
| `entity`     | Database entities and domain enums                                                                                         |
| `events`     | Kafka producers, consumers, event payloads, and event types                                                                |
| `repository` | Spring Data database access                                                                                                |
| `service`    | Transaction, reconciliation, investigation, settlement, caching, and webhook business logic                                |
| `security`   | JWT authentication and tenant context                                                                                      |
| `gateway`    | Payment gateway adapters, routing, results, and health checks                                                              |

## 5. Core Domain Entities

The reviewed backend contains the following important entities:

* `Transaction`
* `ExternalPaymentRecord`
* `ReconciliationRecord`
* `Investigation`
* `ProcessedEvent`
* `FailedEventRecord`
* `User`
* `WebhookConfig`

Related enums represent transaction status, reconciliation status, investigation status, investigation priority, investigation issue type, user role, and settlement event type.

## 6. Main Business Workflows

### 6.1 Transaction Processing

1. The client submits a transaction request.
2. Spring Security authenticates the request where required.
3. The controller delegates processing to the appropriate service.
4. The service interacts with the database and payment gateway abstractions as necessary.
5. Transaction information is persisted in PostgreSQL.
6. Relevant event-driven workflows can use Kafka.
7. Redis caching is used where supported by the existing implementation.

### 6.2 Payment Reconciliation

1. Reconciliation begins through an API request or an internal processing workflow.
2. The service retrieves the internal transaction and corresponding external payment record.
3. It compares amount, currency, and payment status.
4. The reconciliation result is persisted.
5. A discrepancy can trigger an investigation.

The reviewed reconciliation statuses include `MATCHED`, `MISMATCH`, `MISSING_EXTERNAL`, and `MISSING_INTERNAL`.

### 6.3 Investigation Management

1. A reconciliation discrepancy is detected.
2. The investigation service creates or reuses an investigation where appropriate.
3. The investigation records relevant issue information, priority, status, and available transaction or reconciliation references.
4. An authorized user reviews the investigation through the API and frontend.

### 6.4 Event Processing and Recovery

The reviewed event components include transaction producers and consumers, settlement events, a dead-letter consumer, processed-event tracking, and failed-event recovery.

These components support asynchronous processing and recovery. End-to-end delivery guarantees must be established through implementation-specific testing rather than assumed from the architecture alone.

## 7. REST API Areas

The reviewed controllers cover:

* Authentication and registration
* Transaction management
* Payment processing
* Reconciliation
* Investigation management
* External payment simulation
* User management
* Webhook configuration

The reviewed reconciliation endpoints include:

* `POST /api/reconciliation/transactions/{transactionId}`
* `POST /api/reconciliation/external-records/{externalRecordId}`
* `GET /api/reconciliation/transactions/{transactionId}`
* `GET /api/reconciliation/status/{status}`

The reviewed external-payment simulator endpoint is:

* `POST /api/simulator/external-payment/{transactionId}?gateway=...`

These paths describe the reviewed controller mappings. Request and response schemas should be maintained in the API design documentation.

## 8. Database and Schema Management

PostgreSQL is the primary relational database. Flyway manages schema migrations, and Hibernate schema auto-update is disabled.

The reviewed migration history had successful migrations through V11.

Previously reviewed schema areas include:

* Transactions and idempotency
* Reconciliation records
* Investigations
* Processed events
* Multi-tenancy
* User roles
* Payment gateway information
* Failed event records

Already-applied migrations should not be edited to repair existing schema issues. Required schema changes should use a new migration.

### Identified entity mapping issue

The database's `external_payment_records` table contains a required `org_id` column. The reviewed `ExternalPaymentRecord` entity did not map this field.

The reviewed entity constructor and external-payment simulator also did not assign an organization ID.

This is a mismatch between the reviewed Java entity and database schema that must be corrected.

### Identified reconciliation issue

The reviewed missing-internal reconciliation path creates a reconciliation record without assigning its required organization ID.

This can violate the database's non-null constraint.

Both issues require implementation changes and tests before they can be considered resolved.

## 9. Authentication and Tenant Isolation

The reviewed backend uses stateless Spring Security authentication with JWT bearer tokens.

The JWT filter extracts identity, role, and organization information and establishes a tenant context during request processing.

### Risks identified

1. Some reconciliation paths use lookups that are not consistently scoped by organization.
2. The external-payment simulator retrieves transactions by ID without a visible ownership check in the reviewed controller.
3. Investigation retrieval and creation require consistent tenant-scoped authorization.
4. Transaction-cache access must not bypass ownership checks.
5. Authorization decisions must not rely solely on the assumption that a transaction ID or investigation ID is difficult to guess.

These findings identify areas requiring corrective implementation and testing. They do not establish that every endpoint is vulnerable.

**AI security requirement:** The AI investigation service must only receive records the authenticated user is authorized to access.

## 10. Kafka and Redis

### Kafka

Kafka supports asynchronous transaction and settlement workflows. The reviewed code includes event producers, consumers, dead-letter processing, and recovery components.

HTTP request processing and background event processing have different execution contexts. Tenant-scoped API methods must therefore be designed separately from trusted internal processing paths where necessary.

### Redis

The existing `TransactionCacheService` provides transaction caching.

Cache keys and cache access must preserve user and organization boundaries. Authorization must be checked before cached information is returned.

## 11. Architecture Risks and Corrective Actions

| Priority | Finding                                                           | Required action                                        |
| -------- | ----------------------------------------------------------------- | ------------------------------------------------------ |
| High     | Missing `org_id` mapping in the external payment entity           | Add the mapping and populate the organization ID       |
| High     | Missing organization assignment for orphan-payment reconciliation | Populate the reconciliation record's organization ID   |
| High     | Reconciliation access is not consistently tenant-scoped           | Add organization-aware service and repository paths    |
| High     | Simulator transaction lookup lacks a visible ownership check      | Enforce transaction ownership and tenant validation    |
| High     | Investigation access needs consistent tenant isolation            | Scope reads and writes and validate linked records     |
| High     | Cache access needs authorization verification                     | Ensure cache hits cannot bypass ownership checks       |
| Medium   | AI investigation service has not yet been implemented             | Design and implement the AI service                    |
| Medium   | AI provider configuration has not yet been implemented            | Configure the provider and externalize credentials     |
| Medium   | AI workflow tests have not yet been implemented                   | Add service, API, authorization, and integration tests |

These priorities are proposed implementation priorities for the project.

## 12. Planned AI Integration

The AI investigation assistant will follow this logical workflow:

1. An authenticated user requests an explanation for an investigation.
2. The backend verifies access to the investigation and its linked records.
3. A context-building service gathers authorized transaction, external-payment, reconciliation, and investigation evidence.
4. Unnecessary sensitive information is excluded.
5. The AI model receives structured evidence and instructions.
6. The response is validated and returned in a structured format.
7. The frontend displays the summary, supporting evidence, explanation, and recommended next checks.

The proposed response structure includes:

* Investigation summary
* Observed evidence
* Likely explanation
* Recommended next steps
* Missing information and limitations

The model must distinguish observed facts from hypotheses. It must not invent transaction evidence or independently execute financial actions.

## 13. Phase 9 Implementation Sequence

1. Complete the architecture review and document the existing system.
2. Define AI requirements, response schemas, safety rules, and API contracts.
3. Implement the AI context-building and investigation services.
4. Expose authenticated AI endpoints.
5. Integrate the AI investigation interface into React.
6. Test evidence accuracy, error handling, tenant isolation, and the end-to-end workflow.

## 14. Phase 9.1 Completion Criteria

Phase 9.1 documents the existing architecture, major backend components, business workflows, API areas, persistence, security model, and identified risks.

This document completes the documentation deliverable for the architecture review based on the source details reviewed so far.

The identified code defects are not considered fixed until the implementation changes are applied and tests pass.

**Suggested repository location:** `docs/current-architecture.md`
