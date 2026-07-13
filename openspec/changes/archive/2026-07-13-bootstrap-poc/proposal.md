# Proposal: bootstrap-poc

## Why

We want to learn Camunda 8 hands-on by building a small but realistic end-to-end system, structured the same way as our earlier CIB seven + React POC (`cib7-react-poc`) so the two engines can be compared directly. The repo is currently empty; this change bootstraps the whole POC: platform, middleware, frontend, two business processes, and dockerization — implemented step by step in phases.

## What Changes

- Run **Camunda 8.9** locally (single `camunda/camunda` orchestration-cluster container, H2 storage, no Elasticsearch) via Docker Compose.
- Add a **Spring Boot 4 middleware** (`backend/`) that auto-deploys BPMN/DMN/form resources at startup, hosts job workers, and exposes a small `/api/**` REST surface for the frontend (process catalog, start instance, task list, task form, task complete).
- Add an **Angular 22 frontend** (`frontend/`) with pages: Services (start a process), Tasks (open user tasks), Task detail (Camunda Form rendered with `@bpmn-io/form-js-viewer`), Processes (instances + status).
- Add two BPMN processes with Camunda Forms:
  - `vehicle-registration` — start form → job worker price lookup → review user task → end.
  - `business-registration` — start form → DMN auto-approval decision → auto-approve or manual review → end.
- **Dockerize** backend and frontend so `docker compose up --build` brings up the entire stack.
- No authentication in this change (Camunda dev mode, `unprotectedApi: true`); Keycloak/OIDC is a future change.

## Capabilities

### New Capabilities

- `camunda-platform`: Camunda 8.9 orchestration cluster + connectors runtime running locally via Docker Compose, with Operate/Tasklist UIs available for inspection.
- `backend-api`: Spring Boot 4 middleware — process resource deployment, job workers, and the `/api/**` REST contract consumed by the frontend.
- `frontend-app`: Angular SPA — process catalog, task list, form-js task forms, process instance overview; talks only to `backend-api`.
- `vehicle-registration-process`: the simplified vehicle registration business process (BPMN + forms + worker behavior).
- `business-registration-process`: the simplified business registration business process (BPMN + DMN + forms).
- `deployment`: full-stack Docker Compose topology (images, ports, networking, persistence).

### Modified Capabilities

(none — greenfield)

## Impact

- New top-level folders: `backend/`, `frontend/`, `docker/`, `docs/`, plus `docker-compose.yml`.
- New external dependencies: `camunda/camunda:8.9.x` and `camunda/connectors-bundle` images, `io.camunda:camunda-spring-boot-starter:8.9.x`, Angular 22, `@bpmn-io/form-js-viewer` ^1.21.
- Toolchain: requires JDK 21+ locally (current default is JDK 11), Docker, Node 20+, Camunda Desktop Modeler for authoring.
- Business-level specs live in `docs/business/services/**` (analyst-owned markdown); OpenSpec capabilities reference them rather than duplicating field-level detail.
