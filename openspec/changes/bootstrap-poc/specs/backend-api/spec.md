# backend-api

## ADDED Requirements

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
