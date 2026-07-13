# Design: bootstrap-poc

## Context

Empty repo; target architecture mirrors `cib7-react-poc` (CIB seven + React) but on the Camunda 8 stack. Facts verified July 2026: Camunda 8.9 is current (unified `camunda/camunda` image; separate zeebe/operate/tasklist images discontinued; RDBMS/H2 secondary storage is GA — no Elasticsearch needed; v1 Tasklist/Operate APIs deprecated, removed in 8.10). Spring Boot 4.0 GA; the Camunda starter is `io.camunda:camunda-spring-boot-starter` (renamed in 8.9). Angular 22 is current.

Detailed business specs (flows, fields, decision tables, variables) live in `docs/business/services/<service>/` — that markdown is the source of truth for process content; this design covers architecture only.

## Goals / Non-Goals

**Goals:**
- Smallest realistic Camunda 8 system: engine + middleware + SPA + 2 processes, fully dockerized.
- Learn the C8-native way: Orchestration Cluster REST API v2, `CamundaClient`, `@JobWorker`, Camunda Forms, FEEL/DMN in Zeebe.
- Keep the repo AI-friendly: spec-first markdown, OpenSpec change workflow, stable conventions.

**Non-Goals:**
- Authentication/authorization (future change: Keycloak OIDC).
- Production hardening (HTTPS, persistence beyond a named volume, scaling, backups).
- Connectors usage (the connectors runtime runs, but both processes use plain job workers).
- Multi-user task assignment semantics (no assignee/candidate groups until auth exists).

## Decisions

1. **Single orchestration-cluster container with H2** (vs. full compose with Elasticsearch/Keycloak): the 8.9 lightweight distribution is 3 services and starts in seconds; Operate/Tasklist are bundled at `:8080/operate` and `:8080/tasklist`. Elasticsearch variant adds nothing for a POC and is being phased out (8.10 removes bundled ES).
2. **Frontend → backend only; backend → Camunda via `CamundaClient`** (vs. frontend calling `/v2` directly): mirrors the cib7 POC's middleware pattern, gives one place to add auth later, and exercises the Spring starter — a main learning goal. Backend exposes a deliberately thin `/api` facade over the v2 API.
3. **BPMN/DMN/forms live in `backend/src/main/resources/processes/<service>/`** and are auto-deployed with `@Deployment` at startup (vs. a top-level `processes/` folder + deploy script): zero extra tooling, redeploy = restart backend, same pattern the cib7 POC used with its engine module.
4. **Camunda user tasks + Camunda Forms** (`zeebe:userTask`, Modeler default; form schema fetched via `GET /v2/user-tasks/{key}/form`) rendered with `@bpmn-io/form-js-viewer` wrapped in one Angular component. Custom Angular forms (registry pattern) deferred to a possible later change.
5. **DMN with `bindingType="deployment"`** on the business rule task so the decision version always matches the BPMN it deployed with (same rationale as `decisionRefBinding="deployment"` in the cib7 POC).
6. **API v2 only** — no code against the deprecated Tasklist/Operate v1 APIs (removed in 8.10).
7. **Ports**: orchestration 8080 (REST + UIs) / 26500 (gRPC) / 9600 (management), connectors 8086, backend 8085, frontend 3000 (nginx in Docker) / 4200 (ng serve dev, proxying `/api` → 8085).
8. **Dev mode security**: `camunda.security.authentication.method: basic`, `unprotectedApi: true`, `authorizations.enabled: false`, seeded `demo`/`demo` for the bundled UIs. Backend uses `camunda.client.auth.method: none`.

## Risks / Trade-offs

- [8.9 details drift by implementation time] → pin exact image/starter patch versions in one place (`.env` for compose, property in `pom.xml`); re-check release notes when starting Phase 2.
- [Local JDK is 11; Spring Boot 4 needs 17+] → install Temurin JDK 21 before backend work; `mvnw` wrapper removes the Maven 3.6.3 dependency.
- [Unprotected API is wide open] → acceptable for a local POC; documented in `docs/architecture.md`; auth is an explicit future change.
- [H2 file storage is single-node dev-grade] → fine for POC; compose uses a named volume so state survives restarts.
- [form-js inside zoneless Angular 22] → form-js is plain JS (framework-agnostic); wrap in one component, create in `afterNextRender`/`ngAfterViewInit`, destroy on teardown. If change detection quirks appear, trigger explicit signal updates on form events.
- [No auth means no "my tasks"/"my processes" filtering] → task/process lists show everything; spec'd that way on purpose.

## Migration Plan

Greenfield; phases land as ordinary commits on `master` in plan order (platform → backend → frontend → process 1 → process 2 → dockerize). Each phase is independently verifiable (see tasks.md); rollback = git revert.

## Open Questions

- Exact latest 8.9.x / connectors patch tags at Phase 2 time (pin then).
- Whether the connectors runtime is worth keeping in compose from day one (start with it; drop if it distracts).
