# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Learning POC mirroring `C:\Users\kriks\git\cib7-react-poc` (CIB seven + React) on the Camunda 8 stack: **Camunda 8.9** orchestration cluster (Docker, H2 storage — no Elasticsearch) + **Spring Boot 4** middleware (`backend/`) + **Angular 22** SPA (`frontend/`). Two processes: `vehicle-registration` (job worker pricing) and `business-registration` (DMN auto-approval).

## Commands

```bash
# Full stack (everything in Docker)
docker compose up --build -d          # frontend :3000, backend :8085, Camunda :8080/:26500
docker compose down                   # -v wipes Camunda data (clean slate)

# Dev loop (cluster in Docker, apps local)
docker compose up -d orchestration connectors
cd backend && ./mvnw spring-boot:run  # :8085 — needs JDK 21+ (JAVA_HOME, not global PATH java=11)
cd frontend && npm start              # :4200, proxies /api -> :8085 — needs Node >= 22.22.3

# Backend
cd backend && ./mvnw compile          # typecheck
cd backend && ./mvnw test             # tests

# Frontend
cd frontend && npx ng build           # typecheck + build
cd frontend && npx ng test            # vitest unit tests
```

Camunda UIs: http://localhost:8080/operate and `/tasklist`, login `demo`/`demo`. API v2 is unprotected in dev (`docker/camunda/application-h2.yaml`).

## Architecture (read docs/architecture.md for detail)

Browser → Angular (`/api` same-origin; nginx proxy in Docker, ng-serve proxy in dev) → Spring Boot facade (`com.poc.backend.api`, thin controllers + DTO records in `api/dto` with static `from()` mappers) → `CamundaClient` → Orchestration Cluster **REST API v2 only** (v1 Tasklist/Operate APIs are removed in 8.10; client default `prefer-rest-over-grpc=true`, gRPC only for job streaming).

BPMN/DMN/`.form` files live in `backend/src/main/resources/processes/<service>/` and auto-deploy at startup via `@Deployment` on `BackendApplication`. Job workers (`@JobWorker`) in `com.poc.backend.worker`. User tasks are **Camunda user tasks** (`zeebe:userTask`) with **linked forms** (`bindingType="deployment"`); DMN via `zeebe:calledDecision` (`bindingType="deployment"`). Frontend renders form schemas with `@bpmn-io/form-js-viewer` wrapped in `shared/form-viewer.ts`.

**Source of truth chain:** `docs/business/services/<service>/` (analyst markdown: flow, variables, form field tables, DMN tables) → BPMN/DMN/form files must match it → OpenSpec (`openspec/`) governs changes (propose → specs → apply → archive; `/opsx:*` commands).

## Conventions

- kebab-case ids everywhere (process, form, decision, job type); camelCase process variables.
- DTOs are separate records in a `dto` package, never nested in controllers.
- Update the business-spec markdown in the same commit as the BPMN/DMN/form files it describes.

## Gotchas (learned the hard way)

- Camunda's RDBMS secondary storage is **eventually consistent** (~0.5s flush; task completion visibility can lag more). Frontend waits (`core/settle.ts`) after writes before reading lists.
- All `@Deployment` resources deploy as ONE deployment — adding a file bumps versions of unchanged resources too.
- `POST /v2/resources/{key}/deletion` removes a definition from the engine but it lingers in the search view; clean slate = `docker compose down -v`.
- Deleting a process resource file needs `./mvnw clean` (stale copies survive in `target/classes`).
- Process-definition search with `isLatestVersion` filter only sorts by `processDefinitionId`/`tenantId`.
- `httpclient5.version` is pinned to 5.6.x in `backend/pom.xml` (Camunda client needs ≥ 5.6; Spring Boot 4.0 manages 5.5.x).
- Local toolchain: global `java` is 11 — always build via `mvnw` with `JAVA_HOME` pointing to JDK 21 (`C:\Program Files\Java\jdk-21`). Node via nvm, ≥ 22.22.3.
- git-bash curl mangles non-ASCII JSON bodies; test unicode through the browser.
