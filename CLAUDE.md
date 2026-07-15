# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Learning POC mirroring `C:\Users\kriks\git\cib7-react-poc` (CIB seven + React) on the Camunda 8 stack: **Camunda 8.9** orchestration cluster (Docker, H2 storage — no Elasticsearch) + **Spring Boot 4** middleware (`backend/`) + **Angular 22** SPA (`frontend/`) + **Strapi 5** CMS (`cms/`, SQLite) for editorial service-catalog content. Two processes: `vehicle-registration` (job worker pricing) and `business-registration` (DMN auto-approval).

## Commands

```bash
# Full stack (everything in Docker)
docker compose up --build -d          # frontend :3000, backend :8085, Camunda :8080/:26500, Strapi :1337
docker compose down                   # -v wipes Camunda data AND Strapi CMS content (clean slate)

# Dev loop (cluster + Keycloak + CMS in Docker, apps local)
docker compose up -d orchestration connectors keycloak strapi
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

Auth: Keycloak :8180 (admin `admin`/`admin`), realm `camunda-poc` auto-imported from `docker/keycloak/realm-export.json`. App users: `bart`/`bart` (role `applicant`, starts processes), `homer`/`homer` (role `civil-servant`, completes tasks). `/api/**` needs a bearer token — dev curl recipe (password grant) is in README.md.

## Architecture (read docs/architecture.md for detail)

Browser → Keycloak login (keycloak-angular, code + PKCE, `login-required`; bearer interceptor + role route guards in `frontend/src/app/core/`) → Angular (`/api` same-origin; nginx proxy in Docker, ng-serve proxy in dev) → Spring Boot facade (`com.poc.backend.api`, thin controllers + DTO records in `api/dto` with static `from()` mappers) → `CamundaClient` → Orchestration Cluster **REST API v2 only** (v1 Tasklist/Operate APIs are removed in 8.10; client default `prefer-rest-over-grpc=true`, gRPC only for job streaming).

Editorial catalog content: `GET /api/services` joins latest process definitions with Strapi's published `service` entries on `processDefinitionId` (`com.poc.backend.strapi.StrapiClient`, env `STRAPI_URL`, 2s timeout, degrades to engine-only items when Strapi is down). Camunda owns executable artifacts (BPMN/DMN/forms); Strapi owns citizen-facing copy (title, summary, instructions). Content model + seed live in `cms/src/` and are committed like code.

i18n (en + ar/RTL): explicit `?locale=` param on `/api/services` and the form endpoints; `en` (or absent) bypasses translation entirely. Strapi i18n localizes `service` content; `form-translation` (joined on the Camunda form id) holds Arabic form strings that `FormTranslator` overlays onto schemas at read time — presentational strings only (label/description/placeholder/option labels/text), never keys/types/validate/conditionals. Fallbacks: string → authored English; catalog entry → English entry. Frontend: `core/language.service.ts` (localStorage `c8poc.lang`, sets `document.dir`), chrome strings in `core/i18n.ts` (developer-owned, not CMS), CSS uses logical properties for RTL.

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
- Keycloak issuer is pinned to `http://localhost:8180` (`KC_HOSTNAME_URL`) so browser- and container-validated tokens agree on `iss`. The backend fetches JWKS via a reachable URL (`KEYCLOAK_JWKS_URI`, service DNS in Docker) but validates `iss` against the canonical localhost URL — don't "simplify" either side or Docker mode breaks.
- Realm JSON is only imported on first boot of a fresh Keycloak container; after editing `docker/keycloak/realm-export.json`, recreate the container (`docker compose up -d --force-recreate keycloak`) to re-import.
- Keycloak SSO sessions span :3000 and :4200 — logging in on one origin logs you in on the other (same realm cookie). Not a bug when switching between Docker and dev frontends.
- Strapi seeds content only when it is missing (bootstrap hook in `cms/src/index.js`, per-document checks); after editing the fixtures in `cms/src/data/`, re-seed with `docker compose down -v` (wipes everything) or edit via the admin panel.
- Strapi locales are DB entities, not config: the bootstrap hook creates `ar` on boot. Adding a locale in the admin alone won't survive `down -v` — extend the hook instead.
- Adding a field to a `.form` file? Its Arabic label goes into `cms/src/data/seed-form-translations.json` (key = the field's `key`, text views by `id`) — otherwise the field shows authored English under `locale=ar` (by design, not a bug).
- Strapi admin (:1337/admin) asks to register a local admin on first visit — unrelated to Keycloak; seeded content + public read work without it.
- A BuildKit layer cached from a disk-full incident can bake corrupt `node_modules` into an image (symptom: `strapi start` exits 0 silently in a restart loop; direct `node .../strapi.js start` shows `ERR_INVALID_PACKAGE_CONFIG`) — rebuild with `docker compose build --no-cache <service>`.
