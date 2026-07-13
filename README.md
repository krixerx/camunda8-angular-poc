# camunda8-angular-poc

Learning POC: **Camunda 8.9** (process orchestration) + **Spring Boot 4** middleware + **Angular 22** frontend, as an AI-friendly, spec-first monorepo. Sibling project to `cib7-react-poc` (CIB seven + React) — same idea, next-generation engine.

## Quick start

```bash
docker compose up --build -d
```

| URL | What | Login |
|---|---|---|
| http://localhost:3000 | The POC frontend (Services / Tasks / Processes) | — |
| http://localhost:8080/operate | Camunda Operate (instances, diagrams, decisions) | demo / demo |
| http://localhost:8080/tasklist | Camunda Tasklist (bundled) | demo / demo |
| http://localhost:8085/api | Backend REST facade | — |

## What it does

Two BPMN processes run on Camunda 8 and are startable from the frontend:

- **Vehicle registration** — start form → `fetch-vehicle-price` job worker (hardcoded price map) → review user task → Registered/Rejected
- **Business registration** — start form → **DMN decision** `business-auto-approval` (capital ≥ 2500 ∧ adult → auto-approve) → auto-Registered, or manual review → Registered/Rejected

User task forms are **Camunda Forms** (`.form` files deployed with the BPMN), rendered in Angular with `@bpmn-io/form-js-viewer`. The frontend talks only to the Spring Boot middleware (`/api/**`), which talks to Camunda via `CamundaClient` (Orchestration Cluster REST API v2).

## Repo layout

| Folder | Purpose |
|---|---|
| `openspec/` | OpenSpec spec-first workflow (capability specs + change proposals) |
| `docs/` | Architecture + per-service business specs (analyst-owned source of truth) |
| `backend/` | Spring Boot 4 middleware; processes in `src/main/resources/processes/` auto-deploy at startup |
| `frontend/` | Angular 22 SPA (form-js task forms) |
| `docker/camunda/` | Orchestration cluster config (H2, dev security posture) |

## Development

```bash
# cluster only in Docker, apps local with hot reload
docker compose up -d orchestration connectors
cd backend && ./mvnw spring-boot:run    # JDK 21+, http://localhost:8085
cd frontend && npm start                # Node >= 22.22.3, http://localhost:4200
```

Author BPMN/DMN/forms with [Camunda Desktop Modeler](https://camunda.com/download/modeler/); files live in `backend/src/main/resources/processes/<service>/` and redeploy on backend restart. Business-level specs (flows, fields, decision tables) live in `docs/business/services/` — keep them in sync with the process files.

## Deliberate POC trade-offs

No authentication (Camunda dev mode, open API — Keycloak/OIDC is a planned follow-up change), single-node H2 storage, no HTTPS. See `docs/architecture.md`.
