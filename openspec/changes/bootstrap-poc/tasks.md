# Tasks: bootstrap-poc

## 1. Camunda platform (Phase 2)

- [ ] 1.1 Fetch the official Camunda 8.9 lightweight docker-compose distribution; pin latest 8.9.x / connectors patch tags in `.env`
- [ ] 1.2 Adapt it: `docker-compose.yml` at repo root, orchestration config in `docker/camunda/application-h2.yaml` (basic auth, `unprotectedApi: true`, demo/demo user, H2 on named volume)
- [ ] 1.3 Verify: `docker compose up -d` → `GET /v2/topology` OK; Operate + Tasklist UIs log in with demo/demo
- [ ] 1.4 Verify restart persistence: deploy anything via Modeler, `docker compose restart`, definition still present

## 2. Backend middleware skeleton (Phase 3)

- [ ] 2.1 Install/verify JDK 21 locally (`java -version` ≥ 21 for the build shell)
- [ ] 2.2 Scaffold `backend/` — Spring Boot 4.0.x, Java 21, mvnw, deps: webmvc + `io.camunda:camunda-spring-boot-starter:8.9.x`
- [ ] 2.3 Configure `application.yaml` (self-managed mode, gRPC/REST addresses env-overridable, auth none)
- [ ] 2.4 Add `@Deployment` classpath auto-deploy + temporary `hello.bpmn`; verify deployment log and Operate
- [ ] 2.5 Implement `/api` REST facade per backend-api spec (definitions, start-with-form, instances, tasks, task detail + form, complete) with JSON error handling
- [ ] 2.6 Verify with curl: list definitions, start hello instance, list/complete its task; remove `hello.bpmn` once real processes exist

## 3. Frontend skeleton (Phase 4)

- [ ] 3.1 Scaffold `frontend/` — `ng new` Angular 22, standalone, routing, strict; dev proxy `/api` → 8085
- [ ] 3.2 Build the form-js wrapper component (`@bpmn-io/form-js-viewer`, CSS, submit/validation events, teardown)
- [ ] 3.3 Services page: catalog from `/api/process-definitions`, start form dialog/page → start endpoint
- [ ] 3.4 Tasks page + Task detail page (form-js render, complete, navigate back)
- [ ] 3.5 Processes page: instance list with state
- [ ] 3.6 Verify in browser against local backend + cluster

## 4. Process 1: vehicle registration (Phase 5)

- [ ] 4.1 Author in Desktop Modeler: `vehicle-registration.bpmn` + start form + review form (Camunda user tasks, linked forms) into `backend/src/main/resources/processes/vehicle-registration/`
- [ ] 4.2 Implement `fetch-vehicle-price` `@JobWorker` (hardcoded category→price map)
- [ ] 4.3 Verify end-to-end in the UI: start → price set → review task → approve → completed in Operate; also exercise reject path

## 5. Process 2: business registration (Phase 6)

- [ ] 5.1 Author `business-registration.bpmn` + `business-auto-approval.dmn` + start/review forms into `backend/src/main/resources/processes/business-registration/`
- [ ] 5.2 Verify auto-approve run (capital ≥ 2500, adult) ends without user task; manual run creates review task; approve + reject paths both end correctly

## 6. Dockerization + docs (Phase 7)

- [ ] 6.1 `backend/Dockerfile` (multi-stage Maven → JRE 21); wire into compose with `orchestration` service DNS env vars
- [ ] 6.2 `frontend/Dockerfile` (Node build → nginx with `/api` reverse proxy); publish 3000:80
- [ ] 6.3 Full-stack verify: `docker compose up --build` on clean state → both processes end-to-end at `http://localhost:3000`
- [ ] 6.4 Update `docs/architecture.md` if topology drifted; regenerate `CLAUDE.md` with real commands; finalize README
- [ ] 6.5 Archive this OpenSpec change (`openspec archive bootstrap-poc`) → specs promoted to `openspec/specs/`
