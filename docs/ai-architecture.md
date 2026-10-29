# PayFlow AI

## AI Architecture

---

# 1. Purpose

The AI subsystem helps PayFlow automatically investigate inconsistencies between:

* Internal transaction records
* Payment gateway responses
* External settlement records
* Payment events
* Reconciliation results
* Operational logs

The AI is primarily an investigation and decision-support system.

It does not receive unrestricted authority to execute financial operations.

---

# 2. AI Responsibilities

The AI subsystem can:

1. Analyze transaction inconsistencies.
2. Summarize investigation evidence.
3. Identify possible root causes.
4. Classify incident types.
5. Estimate confidence and communicate uncertainty.
6. Recommend possible remediation.
7. Explain the evidence supporting a recommendation.
8. Identify missing evidence.
9. Prioritize investigation cases.

The AI shall not independently:

* Modify payment records.
* Execute refunds.
* Retry payments without backend authorization.
* Modify settlement records.
* Change user permissions.
* Execute arbitrary database queries.

---

# 3. High-Level AI Architecture

```text
                       PayFlow Backend
                              |
                              v
                     Investigation API
                              |
                              v
                    AI Investigation Service
                              |
                              v
                  Tenant Authorization Checks
                              |
                              v
                    Evidence Collector
                              |
                              v
                 Evidence Processor and
                      Context Builder
                              |
                              v
                    AI Provider Adapter
                              |
                              v
                     LLM Provider
                              |
                              v
                   Structured AI Response
                              |
                              v
                    Response Validator
                              |
                              v
                  AI Analysis Persistence
                              |
                              v
                  Authorized API Response
```

AI-generated recommendations remain advisory. Any subsequent financial or operational action must pass through the application's existing authorization, business rules, risk checks, and approval controls.

---

# 4. Architecture Design Principles

The AI investigation subsystem shall follow these principles:

1. **Evidence-grounded analysis:** The model shall analyze authorized application records rather than invent transaction facts.
2. **Tenant isolation:** A record identifier alone shall never grant access to an investigation or its related evidence.
3. **Separation of responsibilities:** Evidence collection, AI provider communication, response validation, and result persistence shall have separate responsibilities.
4. **Provider independence:** Investigation business logic shall not depend directly on provider-specific APIs.
5. **Safe failure:** AI failures shall not corrupt payment, reconciliation, or investigation records.
6. **Auditable results:** Each analysis shall be associated with its investigation and retain appropriate metadata and history.
7. **Human and policy control:** AI output shall remain advisory and shall not bypass authorization, deterministic policies, risk checks, or required approvals.
8. **Data minimization:** Only information required for the investigation shall be sent to the model provider.

---

# 5. AI Component Responsibilities

## 5.1 Investigation API

The existing investigation API shall provide the entry point for authorized users to request AI analysis.

Responsibilities:

* Accept the target investigation identifier.
* Require authentication.
* Delegate analysis to the AI investigation service.
* Return the validated result or an appropriate failure response.
* Avoid accepting a client-supplied organization identifier as proof of authorization.

The exact endpoint, HTTP status codes, and request/response DTOs shall be finalized during API design.

## 5.2 AI Investigation Service

The AI investigation service shall coordinate the analysis workflow.

Responsibilities:

* Validate the analysis request.
* Obtain the trusted organization context.
* Request authorized investigation evidence.
* Invoke the AI integration boundary.
* Validate the analysis result.
* Persist the result and appropriate metadata.
* Communicate failures without changing existing financial records.

It shall not directly execute financial operations based on model output.

## 5.3 Investigation Evidence Collector

The evidence collector shall assemble the relevant information for the requested investigation.

Potential sources include:

* Investigation details.
* Internal transaction information.
* Reconciliation records and mismatch information.
* Authorized payment or event history available to the application.
* Relevant historical investigation results, when useful.

Responsibilities:

* Verify investigation ownership before collecting evidence.
* Verify that related records belong to the same authorized organization.
* Use tenant-scoped database access where applicable.
* Identify unavailable, missing, or contradictory evidence.
* Preserve references to source records.
* Exclude unrelated or unnecessary sensitive information.

The collector shall not treat possession of a database identifier as authorization.

## 5.4 Evidence Processor and Context Builder

The evidence processor shall prepare the collected records for model consumption.

Responsibilities:

* Convert application records into a bounded, structured representation.
* Distinguish recorded facts from derived observations.
* Preserve source references for important findings.
* Identify missing or conflicting information.
* Limit context size.
* Exclude credentials, JWTs, API keys, passwords, and unrelated sensitive data.
* Treat transaction descriptions, gateway messages, and event payloads as untrusted data rather than instructions.

The context builder shall not invent missing transaction or reconciliation facts.

## 5.5 AI Provider Adapter

The AI provider adapter shall isolate Spring AI and provider-specific configuration from investigation business logic.

The initial implementation shall use the OpenAI API through Spring AI, subject to dependency compatibility verification against the project's existing Spring Boot version.

Responsibilities:

* Build the model request from the validated investigation context.
* Apply configured model and generation settings.
* Enforce configured timeouts and bounded retry policies.
* Handle provider errors and rate limits.
* Avoid logging credentials or unnecessary raw payment data.
* Return the provider response for validation.

The model provider shall not receive direct database access or unrestricted application credentials.

## 5.6 Response Validator

The response validator shall verify the model output before it is persisted or returned to the caller.

Validation shall include:

* Required fields are present.
* The response follows the expected structure.
* Field types and length limits are respected.
* Observed facts are distinguishable from hypotheses.
* Recommendations remain advisory.
* Unsupported claims are not treated as verified application facts.
* Malformed or incomplete responses are handled safely.

A structurally valid response shall not automatically be considered factually correct. The application shall preserve evidence references and communicate uncertainty.

## 5.7 AI Analysis Persistence

The persistence component shall store successful analysis results and the metadata needed to associate them with the correct investigation.

The design shall support multiple analyses for one investigation without silently overwriting required history.

The detailed entity, database migration, indexes, retention policy, and versioning strategy shall be finalized during database design and implementation planning.

---

# 6. Investigation Data Flow

The initial workflow shall be user-triggered.

1. An authenticated user requests AI analysis for an investigation.
2. The backend obtains the trusted organization context.
3. The backend retrieves the investigation using tenant-aware access controls.
4. The backend verifies ownership of the related transaction and any reconciliation records.
5. The evidence collector retrieves only authorized and relevant records.
6. The evidence processor constructs a bounded, structured context.
7. The provider adapter sends the context to the configured model.
8. The response validator checks the returned structure and safety constraints.
9. The persistence component stores the analysis and its metadata.
10. The API returns the validated result to the authorized caller.

If authorization fails, evidence collection and model invocation shall not proceed.

If evidence is insufficient, the system shall communicate that limitation rather than fabricate facts.

If the provider fails or returns an invalid response, the system shall report the analysis as unavailable or incomplete without corrupting existing financial records.

Automatic background-triggered AI analysis is outside the initial implementation scope.

---

# 7. Tenant Isolation and Security Boundaries

## 7.1 Trusted Organization Context

The organization context shall originate from the authenticated security context, not from an untrusted request parameter.

The existing JWT authentication and `TenantContext` mechanisms provide a starting point. Their integration with investigation-specific authorization must be verified before implementation.

Authentication alone shall not be treated as proof that a user can access every investigation.

## 7.2 Tenant-Scoped Record Access

The backend shall enforce organization ownership when retrieving:

* Investigations.
* Transactions.
* Reconciliation records.
* Historical AI analyses.
* Other authorized evidence records used by the investigation.

Where related records are retrieved separately, the application shall verify that their organization ownership is consistent with the authorized investigation.

Existing repository methods that do not explicitly filter by organization shall be reviewed alongside their service and controller call paths. They shall not be assumed safe solely because authentication is enabled.

External payment evidence requires special ownership validation because the current `ExternalPaymentRecord` entity does not contain an organization identifier.

For records linked to an internal transaction, the application shall verify that the transaction belongs to the authorized organization and that the external record is associated with that transaction.

For unmatched external payment records, the application shall establish organization ownership through a validated ingestion or ownership mechanism before using those records in an AI investigation. The application shall not infer ownership from an unrelated record or invent an organization identifier.

The evidence collector shall reject or exclude records whose ownership or relationship cannot be verified.

## 7.3 Asynchronous Execution

Thread-local organization context does not automatically propagate to another thread.

If asynchronous analysis is introduced, the required tenant context must be propagated explicitly or represented in a validated job context. The worker must revalidate access to the relevant records rather than trusting unverified identifiers.

The initial implementation may remain synchronous if that is the simplest safe design. Any asynchronous execution strategy shall be decided explicitly.

## 7.4 Financial Safety

AI-generated recommendations shall not directly:

* Create or alter payments.
* Execute refunds.
* Retry payments.
* Modify settlement or reconciliation records.
* Change permissions.
* Resolve investigations without the required application workflow.

Any subsequent action must pass through the existing authorization, deterministic business rules, risk checks, approval requirements, and audit controls.

## 7.5 Prompt Injection and Untrusted Evidence

Transaction descriptions, gateway responses, event payloads, and other externally supplied text shall be treated as untrusted data.

The application shall clearly separate trusted system instructions from investigation evidence. Instructions embedded in evidence shall not be allowed to override the application's security rules or authorize financial actions.

The AI provider shall not be trusted to enforce tenant isolation. Tenant authorization must be enforced by the backend before evidence is collected and sent to the provider.

---

# 8. AI Input and Output Contracts

## 8.1 Investigation Input Context

The internal model context should contain only the relevant information available and authorized for the investigation.

Proposed logical fields include:

* Investigation identifier and issue type.
* Relevant investigation summary and status.
* Internal transaction status and necessary transaction attributes.
* Relevant reconciliation status and mismatch details.
* Relevant event or gateway evidence, when available.
* Source references for collected evidence.
* Missing or contradictory evidence.
* Explicit instructions to distinguish recorded facts from hypotheses.

This is a logical design, not the final Java DTO or API schema.

## 8.2 Structured AI Output

The validated response shall contain:

* `summary`: Concise description of the issue.
* `observedEvidence`: Facts supported by supplied records.
* `likelyExplanation`: Probable cause, marked as a hypothesis when unproven.
* `recommendedNextSteps`: Practical investigation steps for an authorized operator.
* `missingInformation`: Relevant evidence that is unavailable.
* `limitations`: Uncertainty or constraints affecting the analysis.

The final Java types, validation rules, serialization format, and public API schema shall be specified during API design.

---

# 9. AI Analysis Persistence and History

Each successful analysis shall be associated with its investigation and organization.

The persistence design shall consider:

* Analysis identifier.
* Investigation identifier.
* Organization identifier.
* Analysis status.
* Structured result.
* Creation and completion timestamps.
* Provider and model metadata.
* Relevant source evidence references.
* Error classification for failed analyses, where applicable.

Repeated analysis shall preserve required history instead of silently replacing previous results.

Retrieval of analysis history shall enforce the same tenant authorization as the original investigation.

The exact database schema, indexes, retention rules, and migration shall be defined before implementation.

The implementation shall also consider how to handle a provider failure, a persistence failure, and a client retry without incorrectly presenting an incomplete analysis as successful.

---

# 10. Failure Handling and Reliability

## 10.1 Authorization Failure

The application shall reject unauthorized analysis requests before collecting evidence or invoking the model.

## 10.2 Insufficient Evidence

The analysis shall disclose missing information and limitations. The system shall not invent evidence to fill gaps.

## 10.3 Provider Timeout or Failure

Model calls shall use configurable timeouts and bounded retries.

Provider failure shall not prevent ordinary payment processing, reconciliation persistence, or investigation creation.

## 10.4 Invalid Model Response

Malformed, incomplete, or unacceptable responses shall not be persisted as successful analyses.

The application shall report an appropriate analysis failure or incomplete result.

## 10.5 Duplicate Requests

The implementation shall consider repeated requests, retries, and persistence failures so that a single request does not unintentionally create uncontrolled duplicate analyses.

The exact idempotency strategy shall be finalized during implementation design.

## 10.6 Logging and Observability

Operational records should support tracing an analysis to its investigation and identifying failure categories.

Logs shall avoid provider secrets, tokens, and unnecessary raw payment data.

Metrics may include:

* Analysis success and failure counts.
* Analysis latency.
* Provider timeout rates.
* Provider error categories.
* Retry counts.

---

# 11. Testing Requirements

The AI investigation implementation shall include tests for:

1. Successful analysis using authorized evidence.
2. Missing or insufficient evidence.
3. Investigation belonging to another organization.
4. Related transaction or reconciliation records belonging to another organization.
5. Malformed model output.
6. Provider timeout and failure.
7. Retry behavior and duplicate requests.
8. Persistence and retrieval of multiple analyses.
9. Tenant isolation when retrieving analysis history.
10. Confirmation that AI recommendations cannot bypass financial authorization or approval controls.
11. Prompt-injection attempts embedded in transaction descriptions or external evidence.
12. Ensuring sensitive credentials and unrelated payment data are excluded from model input and operational logs.

Tests shall verify both successful behavior and safe failure.

---

# 12. Architecture Decisions and Deferred Work

The following decisions define the initial scope:

* Spring AI with the OpenAI API is the initial provider integration.
* Provider and model configuration shall be externalized.
* A vector database and RAG pipeline are not required initially.
* Analysis is initiated by an authorized user.
* Investigation evidence comes from authorized application records.
* Analysis results shall retain required history.
* AI remains advisory and cannot independently execute financial operations.
* Backend authorization, not the AI provider, enforces tenant isolation.

The following details remain for subsequent design and implementation:

* Final Java DTOs and API schema.
* Exact AI result entity and database migration.
* Spring AI dependency versions and Spring Boot compatibility.
* Provider timeout, retry, and output-size configuration.
* Detailed retention and versioning policy.
* Whether analysis execution remains synchronous or becomes asynchronous.
* Detailed metrics, audit events, and evaluation datasets.
* The precise strategy for handling duplicate analysis requests and persistence failures.

---

**Status:** AI architecture documented for review. Implementation has not started.

**Next step:** Review the document and Git diff, resolve remaining design decisions, and finalize Phase 9.3 before beginning Phase 9.4.
