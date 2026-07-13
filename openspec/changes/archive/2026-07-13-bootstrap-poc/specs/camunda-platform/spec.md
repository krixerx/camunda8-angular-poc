# camunda-platform

## ADDED Requirements

### Requirement: Camunda 8.9 orchestration cluster runs locally via Docker Compose
The repository SHALL provide a `docker-compose.yml` that starts a Camunda 8.9 orchestration cluster (single `camunda/camunda` container: Zeebe + Operate + Tasklist + Identity) using H2 secondary storage, without Elasticsearch. Image versions SHALL be pinned in a `.env` file.

#### Scenario: Cluster starts healthy
- **WHEN** the developer runs `docker compose up -d`
- **THEN** `GET http://localhost:8080/v2/topology` returns the broker topology within 2 minutes

#### Scenario: Bundled web UIs are reachable
- **WHEN** the cluster is up and the developer opens `http://localhost:8080/operate` and `http://localhost:8080/tasklist`
- **THEN** both UIs load and accept the seeded `demo`/`demo` login

### Requirement: Dev-mode security posture
The orchestration cluster SHALL run with `authentication.method: basic`, `unprotectedApi: true`, and authorizations disabled, so the REST API (8080) and gRPC gateway (26500) are usable without tokens. This posture SHALL be documented as POC-only in `docs/architecture.md`.

#### Scenario: API usable without credentials
- **WHEN** a client calls `POST http://localhost:8080/v2/process-definitions/search` with no Authorization header
- **THEN** the request succeeds (HTTP 200)

### Requirement: Process state survives restarts
Camunda data SHALL be stored on a named Docker volume so deployed definitions and instance state survive `docker compose restart`.

#### Scenario: Restart keeps deployments
- **WHEN** a process is deployed, and the compose stack is stopped and started again (without `-v`)
- **THEN** the process definition is still listed in Operate

### Requirement: Connectors runtime is available
The compose stack SHALL include the `camunda/connectors-bundle` runtime connected to the orchestration cluster (not used by the POC processes yet, but available for later experiments).

#### Scenario: Connectors runtime connects
- **WHEN** the stack is up
- **THEN** the connectors container logs show a successful connection to the orchestration cluster
