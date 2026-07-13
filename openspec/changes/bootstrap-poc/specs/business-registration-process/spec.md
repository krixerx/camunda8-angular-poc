# business-registration-process

Business detail (fields, variables, decision table) is specified in `docs/business/services/business-registration/` — that markdown is the source of truth for process content. This spec covers the executable behavior.

## ADDED Requirements

### Requirement: Business registration process is deployed and startable
A BPMN process with id `business-registration` SHALL be deployed from the backend classpath (together with its DMN) and appear in the frontend Services catalog. It SHALL have a linked Camunda start form collecting company name, share capital, and founder age.

#### Scenario: Process appears in catalog
- **WHEN** the backend has started against a running cluster
- **THEN** `GET /api/process-definitions` includes `business-registration`

### Requirement: DMN decides auto-approval
A business rule task SHALL call decision `business-auto-approval` with `bindingType="deployment"`, producing the variable `autoApproved` (boolean). The decision SHALL auto-approve when share capital ≥ 2500 AND founder age ≥ 18 (see decision spec in docs for the full table).

#### Scenario: Auto-approval granted
- **WHEN** an instance is started with `shareCapital = 2500` and `founderAge = 30`
- **THEN** the DMN evaluates to `autoApproved = true` and the instance ends at "Registered" with no user task created

#### Scenario: Manual review required
- **WHEN** an instance is started with `shareCapital = 1000`
- **THEN** `autoApproved = false` and a `review-application` user task is created

### Requirement: Manual review path
When not auto-approved, a Camunda user task `review-application` SHALL present the application data with an approve/reject choice (`approved` boolean); an exclusive gateway SHALL route to end event "Registered" on approval, else "Rejected".

#### Scenario: Reviewer approves
- **WHEN** the review task completes with `approved = true`
- **THEN** the instance ends at "Registered"

#### Scenario: Reviewer rejects
- **WHEN** the review task completes with `approved = false`
- **THEN** the instance ends at "Rejected"

### Requirement: DMN deploys with the process
The `business-auto-approval.dmn` file SHALL be deployed in the same backend startup deployment as the BPMN, and the decision SHALL be evaluated inside the Zeebe engine (no external decision service).

#### Scenario: Decision visible after deploy
- **WHEN** the backend starts
- **THEN** the decision definition `business-auto-approval` is deployed alongside `business-registration`
