# vehicle-registration-process Specification

## Purpose
TBD - created by archiving change bootstrap-poc. Update Purpose after archive.
## Requirements
### Requirement: Vehicle registration process is deployed and startable
A BPMN process with id `vehicle-registration` SHALL be deployed from the backend classpath and appear in the frontend Services catalog. It SHALL have a linked Camunda start form collecting owner and vehicle data (see business spec).

#### Scenario: Process appears in catalog
- **WHEN** the backend has started against a running cluster
- **THEN** `GET /api/process-definitions` includes `vehicle-registration`

### Requirement: Price lookup runs as a job worker
After the start event, a service task with job type `fetch-vehicle-price` SHALL invoke a backend `@JobWorker` that resolves a registration price from a hardcoded lookup keyed by vehicle category, writing the variable `price` (number) to the process.

#### Scenario: Price resolved automatically
- **WHEN** an instance is started with a known vehicle category
- **THEN** the instance advances past the service task with a numeric `price` variable set, without human action

### Requirement: Review user task gates the outcome
The process SHALL contain a Camunda user task `review-registration` with a linked Camunda Form showing the submitted data and price, and an approve/reject choice writing the variable `approved` (boolean). An exclusive gateway SHALL route to end event "Registered" when `approved = true`, else to end event "Rejected".

#### Scenario: Approval path
- **WHEN** the review task is completed with `approved = true`
- **THEN** the instance ends at "Registered" and is shown completed in Operate

#### Scenario: Rejection path
- **WHEN** the review task is completed with `approved = false`
- **THEN** the instance ends at "Rejected"

