# deployment Specification

## Purpose
TBD - created by archiving change bootstrap-poc. Update Purpose after archive.
## Requirements
### Requirement: One command brings up the full stack
`docker compose up --build` from the repo root SHALL build and start the complete system: orchestration cluster, connectors runtime, Keycloak (with realm auto-import), Strapi (with seeded service catalog content), backend, and frontend. No other manual step SHALL be required for the end-to-end flow. The backend SHALL wait for Keycloak health before starting so JWT validation is available from the first request. The backend SHALL NOT hard-depend on Strapi health (the catalog degrades gracefully to engine-only data).

#### Scenario: Full stack end-to-end
- **WHEN** the developer runs `docker compose up --build` on a clean checkout and waits for health
- **THEN** opening `http://localhost:3000` presents the Keycloak login; logging in as `bart` allows starting both processes, and logging in as `homer` allows completing their review tasks

#### Scenario: Catalog content present out of the box
- **WHEN** the full stack is up on a clean checkout and `bart` opens the Services page
- **THEN** the service cards show the seeded editorial titles and summaries from Strapi

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
Host ports SHALL be: 3000 frontend, 8080 orchestration REST + Operate/Tasklist UIs, 26500 gRPC, 8085 backend (dev convenience), 8086 connectors, 8180 Keycloak, 1337 Strapi (admin panel + API). The map SHALL be documented in `docs/architecture.md`.

#### Scenario: Documented ports respond
- **WHEN** the stack is up
- **THEN** each documented port serves its documented component, including Keycloak on 8180 and Strapi on 1337

### Requirement: Dev loop works with Dockerized Keycloak
With only the infrastructure services running in Docker (`docker compose up -d orchestration connectors keycloak strapi`), a locally run backend (`./mvnw spring-boot:run`) and frontend (`npm start`) SHALL authenticate against the same realm at `http://localhost:8180` and read catalog content from Strapi at `http://localhost:1337` without configuration changes beyond documented defaults (JWKS/issuer/Strapi URLs env-overridable, defaulting to localhost).

#### Scenario: Local apps against Dockerized Keycloak
- **WHEN** the developer runs the dev loop with Keycloak in Docker
- **THEN** login at `http://localhost:4200` works and the local backend accepts the issued tokens

#### Scenario: Local backend reads Dockerized Strapi
- **WHEN** the developer runs the dev loop with Strapi in Docker and opens the Services page at `http://localhost:4200`
- **THEN** the service cards show the editorial content served through the local backend

