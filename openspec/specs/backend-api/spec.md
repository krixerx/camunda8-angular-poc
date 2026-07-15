# backend-api Specification

## Purpose
TBD - created by archiving change bootstrap-poc. Update Purpose after archive.
## Requirements
### Requirement: Spring Boot 4 middleware connects to Camunda
The `backend/` module SHALL be a Spring Boot 4.0.x application (Java 21, Maven wrapper) using `io.camunda:camunda-spring-boot-starter` 8.9.x with `camunda.client.mode: self-managed`, connecting to the orchestration cluster over gRPC (26500) and REST (8080). Connection addresses SHALL be configurable via properties/environment so the same image works locally and in Docker.

#### Scenario: Backend starts against local cluster
- **WHEN** the cluster is up and the developer runs `./mvnw spring-boot:run` in `backend/`
- **THEN** the application starts without errors and the Camunda client connects

### Requirement: Process resources auto-deploy at startup
All `.bpmn`, `.dmn`, and `.form` files under `backend/src/main/resources/processes/**` SHALL be deployed to Camunda automatically at application startup (`@Deployment` classpath pattern).

#### Scenario: Resources deployed on boot
- **WHEN** the backend starts with process files on the classpath
- **THEN** the startup log reports each deployed resource and the definitions appear in Operate

### Requirement: REST facade for the frontend
The backend SHALL expose the following JSON endpoints under `/api`, implemented against the Orchestration Cluster REST API v2 (no deprecated v1 Tasklist/Operate APIs):

| Endpoint | Purpose |
|---|---|
| `GET /api/process-definitions` | List latest deployed process definitions (id, name, version, key) |
| `GET /api/process-definitions/{processDefinitionKey}/form` | Start form schema for a definition (or 204 if none) |
| `POST /api/process-definitions/{processDefinitionKey}/start` | Start an instance with JSON variables; returns instance key |
| `GET /api/process-instances` | List process instances with state (active/completed) |
| `GET /api/tasks` | List user tasks in state CREATED (key, name, processName, creationDate) |
| `GET /api/tasks/{userTaskKey}` | Task detail including its Camunda Form schema and current variables |
| `POST /api/tasks/{userTaskKey}/complete` | Complete the task with JSON variables |

#### Scenario: Start a process instance
- **WHEN** the frontend posts variables to `/api/process-definitions/{key}/start`
- **THEN** the backend creates the instance via CamundaClient and returns HTTP 200 with the process instance key

#### Scenario: Complete a user task
- **WHEN** the frontend posts form values to `/api/tasks/{userTaskKey}/complete`
- **THEN** the task is completed in Camunda and subsequent `GET /api/tasks` no longer lists it

#### Scenario: Task detail includes form schema
- **WHEN** the frontend requests `/api/tasks/{userTaskKey}` for a task with a linked Camunda Form
- **THEN** the response contains the form schema JSON and the task's current variables

### Requirement: Job workers host service task logic
Service task logic (e.g., vehicle price lookup) SHALL be implemented as `@JobWorker` methods in the backend with `@Variable` parameter binding; workers SHALL complete jobs with result variables.

#### Scenario: Worker completes a service task
- **WHEN** a process instance reaches a service task whose type matches a registered worker
- **THEN** the worker executes and the instance advances with the worker's result variables

### Requirement: Errors surface as structured JSON
`/api` endpoints SHALL return structured JSON errors (status, message) rather than opaque 500s when Camunda calls fail (e.g., unknown task key → 404).

#### Scenario: Unknown task
- **WHEN** the frontend requests `/api/tasks/999999` for a non-existent task
- **THEN** the backend returns HTTP 404 with a JSON error body

### Requirement: API requires a valid Keycloak JWT
All `/api/**` endpoints SHALL require a valid bearer token issued by the `camunda-poc` Keycloak realm. The backend SHALL validate the token as an OAuth2 resource server (Spring Security): signature against the realm JWKS (URL env-overridable for Docker vs. local runs) and issuer against the canonical `http://localhost:8180/realms/camunda-poc`. Requests without a valid token SHALL receive HTTP 401 with the structured JSON error body.

#### Scenario: Unauthenticated request rejected
- **WHEN** a client calls `GET /api/tasks` without an `Authorization` header
- **THEN** the backend returns HTTP 401 with a JSON error body

#### Scenario: Authenticated read succeeds for any role
- **WHEN** a client calls `GET /api/process-definitions` with a valid token for `bart` or `homer`
- **THEN** the backend returns HTTP 200 with the definition list

### Requirement: Write endpoints enforce roles
Realm roles from the token (`realm_access.roles`) SHALL be mapped to Spring authorities. `POST /api/process-definitions/{key}/start` SHALL require role `applicant`; `POST /api/tasks/{key}/complete` SHALL require role `civil-servant`. A valid token lacking the required role SHALL receive HTTP 403 with the structured JSON error body.

#### Scenario: Applicant starts a process
- **WHEN** `bart` (role `applicant`) posts to `/api/process-definitions/{key}/start`
- **THEN** the instance is created and HTTP 200 returned

#### Scenario: Civil servant cannot start a process
- **WHEN** `homer` (role `civil-servant`, not `applicant`) posts to `/api/process-definitions/{key}/start`
- **THEN** the backend returns HTTP 403 with a JSON error body and no instance is created

#### Scenario: Civil servant completes a task
- **WHEN** `homer` posts form values to `/api/tasks/{userTaskKey}/complete`
- **THEN** the task is completed and HTTP 200 returned

#### Scenario: Applicant cannot complete a task
- **WHEN** `bart` posts to `/api/tasks/{userTaskKey}/complete`
- **THEN** the backend returns HTTP 403 with a JSON error body and the task remains open

### Requirement: Service catalog endpoint merges Camunda and Strapi
The backend SHALL expose `GET /api/services` returning one `ServiceCatalogItem` per latest deployed process definition, merging engine data (id, name, version, key) with the matching published Strapi `service` entry (title, summary, instructions, whatYouNeed, expectedDuration) joined on `processDefinitionId`. Strapi SHALL be read via a dedicated client with an env-overridable base URL (localhost in dev, service DNS in Docker) and a short request timeout. Content fields SHALL be null when no Strapi entry matches. The endpoint SHALL require a valid bearer token (any realm role), consistent with other read endpoints. `ServiceCatalogItem` SHALL follow the project DTO convention: a separate record in the `dto` package with a static `from()` mapper.

#### Scenario: Merged catalog item
- **WHEN** the frontend requests `GET /api/services` while `vehicle-registration` is deployed and has a published Strapi entry
- **THEN** the response item for `vehicle-registration` carries both the engine fields and the editorial content fields

#### Scenario: Definition without CMS content
- **WHEN** a process definition is deployed with no matching published Strapi entry
- **THEN** the catalog item is still returned with engine fields populated and content fields null

#### Scenario: Strapi unavailable degrades gracefully
- **WHEN** the frontend requests `GET /api/services` while Strapi is down or times out
- **THEN** the backend returns HTTP 200 with engine-only items (all content fields null) and logs a warning — it does not return an error

#### Scenario: Orphaned CMS entry not shown
- **WHEN** a published Strapi entry references a `processDefinitionId` that has no deployed process definition
- **THEN** no catalog item is produced for it

