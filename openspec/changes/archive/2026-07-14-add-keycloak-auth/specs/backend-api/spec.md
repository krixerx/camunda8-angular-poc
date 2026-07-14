# backend-api Specification (delta)

## ADDED Requirements

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
