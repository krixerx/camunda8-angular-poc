# deployment

## ADDED Requirements

### Requirement: One command brings up the full stack
`docker compose up --build` from the repo root SHALL build and start the complete system: orchestration cluster, connectors runtime, backend, and frontend. No other manual step SHALL be required for the end-to-end flow.

#### Scenario: Full stack end-to-end
- **WHEN** the developer runs `docker compose up --build` on a clean checkout and waits for health
- **THEN** opening `http://localhost:3000` allows starting both processes and completing their tasks

### Requirement: Backend image
`backend/Dockerfile` SHALL be a multi-stage build (Maven build stage → JRE 21 runtime stage). In compose, the backend SHALL reach the cluster via service DNS (`http://orchestration:26500` / `http://orchestration:8080`) configured through environment variables.

#### Scenario: Backend container deploys processes
- **WHEN** the compose stack starts
- **THEN** the backend container connects to the orchestration service and deploys the BPMN/DMN/form resources

### Requirement: Frontend image
`frontend/Dockerfile` SHALL be a multi-stage build (Node build → nginx). nginx SHALL serve the SPA on port 80 (published as 3000) and reverse-proxy `/api` to the backend service, so the browser uses same-origin requests (no CORS).

#### Scenario: SPA served with working API
- **WHEN** the stack is up and the browser loads `http://localhost:3000`
- **THEN** the SPA loads and `/api/process-definitions` returns data through the nginx proxy

### Requirement: Port map is documented and stable
Host ports SHALL be: 3000 frontend, 8080 orchestration REST + Operate/Tasklist UIs, 26500 gRPC, 8085 backend (dev convenience), 8086 connectors. The map SHALL be documented in `docs/architecture.md`.

#### Scenario: Documented ports respond
- **WHEN** the stack is up
- **THEN** each documented port serves its documented component
