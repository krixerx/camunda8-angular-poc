# Proposal: add-keycloak-auth

## Why

The POC currently has no authentication — anyone can start processes and complete review tasks, and the business docs explicitly defer "Roles / authorization" to a later phase. The mirror project (`cib7-react-poc`) already models this with Keycloak and the same demo personas; adding Keycloak here completes the mirroring goal and unlocks role-based behavior (applicant vs. civil servant) that real Camunda solutions need.

## What Changes

- Add **Keycloak** as a separate microservice in `docker-compose.yml` (dedicated container, dev-mode with realm auto-import, own host port so the JWT issuer URL is stable for both browser and containers).
- Add a **realm export** with realm roles `applicant` and `civil-servant` and two demo users: `bart`/`bart` (applicant) and `homer`/`homer` (civil servant), plus a public SPA client for the Angular frontend.
- **Backend** becomes an OAuth2 **resource server**: all `/api/**` endpoints require a valid Keycloak JWT; realm roles are mapped to Spring authorities. Role enforcement: starting processes requires `applicant`, completing user tasks requires `civil-servant`; read endpoints require any authenticated user. **BREAKING**: unauthenticated `/api` calls return 401.
- **Frontend** logs in via Keycloak (OIDC Authorization Code + PKCE through `keycloak-js`/`keycloak-angular`), attaches the bearer token to `/api` calls, shows the logged-in user + logout, and gates UI by role (start-process UX for applicants, task-list UX for civil servants).
- Business docs (`docs/business/services/*/README.md` "Roles / authorization" sections) updated to state the new role model, in the same change.
- Dev loop keeps working: Keycloak runs in Docker; local backend/frontend point at `http://localhost:8180`.

Out of scope: securing the Camunda cluster itself (Operate/Tasklist stay on `demo`/`demo`; the v2 API stays unprotected in dev), task assignment/candidate groups inside BPMN, user self-registration, token refresh edge-cases beyond what the libraries give for free.

## Capabilities

### New Capabilities

- `authentication`: Keycloak identity service — realm, roles, demo users, SPA client, issuer configuration, and the end-to-end login contract (OIDC code+PKCE for the SPA, JWT bearer validation in the backend, role semantics of `applicant` and `civil-servant`).

### Modified Capabilities

- `backend-api`: `/api/**` requires a valid JWT (was: unauthenticated); write endpoints enforce roles (`applicant` starts processes, `civil-servant` completes tasks); errors surface as 401/403 JSON.
- `frontend-app`: unauthenticated users are redirected to Keycloak login; bearer token attached to all `/api` requests; role-based UI gating; visible user identity + logout.
- `deployment`: compose stack gains a `keycloak` service (health-checked, realm auto-import); backend/frontend configuration wired to it in both full-Docker and dev-loop modes.

## Impact

- `docker-compose.yml` + new `docker/keycloak/realm-export.json` (or `keycloak/` dir mirroring cib7 layout).
- `backend/`: new `spring-boot-starter-oauth2-resource-server` dependency, `SecurityConfig`, role-to-authority mapping, `application.yaml` issuer/JWKS properties (env-overridable for Docker vs. local).
- `frontend/`: new `keycloak-js` (+ `keycloak-angular`) dependency, auth bootstrap in `app.config.ts`, HTTP interceptor, route guards, header UI; `proxy.conf` untouched (Keycloak is called directly at :8180, not via `/api`).
- `docs/architecture.md`, `docs/business/services/*/README.md`, root `README.md` (login instructions), `CLAUDE.md` (commands/gotchas if any).
- Existing curl-based smoke testing of `/api` now needs a token — document a password-grant/`curl` recipe or keep a dev note.
