# camunda8-angular-poc

Learning POC: **Camunda 8.9** (process orchestration) + **Spring Boot 4** middleware + **Angular 22** frontend, as an AI-friendly, spec-first monorepo. Sibling project to [cib7-react-poc](https://github.com/krixerx) (CIB seven + React) — same idea, next-generation engine.

## What it does

Two BPMN processes run on Camunda 8 and are visible/startable from the Angular frontend:

- **Vehicle registration** (simplified) — start form → price lookup job worker → review user task
- **Business registration** (simplified) — start form → DMN auto-approval decision → auto-approve or manual review

User task forms are Camunda Forms (designed in Desktop Modeler), rendered in Angular with `@bpmn-io/form-js-viewer`. The frontend talks only to the Spring Boot middleware (`/api/**`), which talks to Camunda via the official Spring Boot starter (`CamundaClient`, Orchestration Cluster REST API v2).

## Repo layout

| Folder | Purpose |
|---|---|
| `openspec/` | OpenSpec spec-first workflow (specs + change proposals) |
| `docs/` | Architecture + per-service business specs (analyst-owned markdown) |
| `backend/` | Spring Boot 4 middleware; BPMN/DMN/forms in `src/main/resources/processes/` (auto-deployed at startup) |
| `frontend/` | Angular 22 SPA |
| `docker/` | Camunda orchestration cluster configuration |

## Status

Spec phase — see `docs/` and `openspec/changes/`. Implementation phases are tracked in the OpenSpec change proposals.

## Prerequisites

- Docker (Camunda 8.9 runs via `docker-compose.yml`)
- JDK 21+ (backend)
- Node 20+ (frontend)
- [Camunda Desktop Modeler](https://camunda.com/download/modeler/) for authoring BPMN/DMN/forms
