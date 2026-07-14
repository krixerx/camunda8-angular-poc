# Design: add-keycloak-auth

## Context

The stack (Angular SPA → Spring Boot facade → Camunda 8.9) has no authentication; the business docs defer roles to a later phase. The mirror project `cib7-react-poc` already runs Keycloak 26 in Docker with realm auto-import, the same personas (`bart`/applicant, `homer`/civil-servant), a public SPA client, and a Spring resource-server backend — we reuse its proven decisions wherever they translate, and simplify where cib7's extra trust levels (internal engine tokens, public tokenized URLs) don't apply here.

Constraints:
- Backend runs both in Docker (service DNS) and locally via `mvnw spring-boot:run` — Keycloak URLs must work from both.
- Frontend runs behind nginx (:3000, Docker) and ng-serve (:4200, dev) — both origins must be valid redirect URIs.
- JWT `iss` claim is baked in by Keycloak at token issue time; browser and backend must agree on one issuer string.

## Goals / Non-Goals

**Goals:**
- Keycloak as its own compose service, zero manual setup (realm imported on boot).
- Login required for the whole SPA; bearer token on every `/api` call.
- Role model: `applicant` starts processes, `civil-servant` completes user tasks; reads for any authenticated user.
- Both run modes (full Docker, dev loop) keep working with the same realm.

**Non-Goals:**
- Securing the Camunda cluster itself (Operate/Tasklist stay `demo`/`demo`, v2 API unprotected in dev).
- BPMN-level task assignment / candidate groups (later learning step).
- Production hardening: HTTPS, `start` (non-dev) Keycloak mode, external DB for Keycloak, secret management.
- Custom login theme (cib7 has one; skip here).

## Decisions

### D1: Keycloak 26 in dev mode with realm auto-import, host port 8180
`quay.io/keycloak/keycloak:26.x`, `start-dev --import-realm`, realm JSON mounted at `/opt/keycloak/data/import/`, published as `8180:8080` (8080 is taken by orchestration). Health-checked via the management port (9000) TCP probe so dependents can wait on it. Realm file lives at `docker/keycloak/realm-export.json` next to the existing `docker/camunda/` config — this repo keeps infra config under `docker/`, unlike cib7's top-level `keycloak/`.

*Alternative considered:* configuring users by hand in the admin console — rejected, violates "one command brings up the stack".

### D2: One canonical issuer URL — `http://localhost:8180`
Set `KC_HOSTNAME_URL=http://localhost:8180` so every token carries `iss=http://localhost:8180/realms/<realm>` regardless of where it is inspected. This is the standard fix cib7 uses for the browser-vs-container hostname mismatch.

The backend therefore must NOT use Spring's `issuer-uri` autodiscovery alone (inside Docker, `localhost:8180` is unreachable at startup). Instead:
- `jwk-set-uri` points at a reachable address: `http://keycloak:8080/realms/<realm>/protocol/openid-connect/certs` in Docker, `http://localhost:8180/...` locally (env-overridable, same pattern as `CAMUNDA_REST_ADDRESS`).
- Issuer is validated by string-compare against the canonical public URL (custom `JwtDecoder` with `JwtValidators.createDefaultWithIssuer`), mirroring cib7's approach.

*Alternative considered:* `host.docker.internal` as the single URL — works on Docker Desktop only, and makes local dev depend on Docker DNS quirks. Rejected.

### D3: Realm content
Realm `camunda-poc`; realm roles `applicant`, `civil-servant`; users `bart`/`bart` (applicant) and `homer`/`homer` (civil-servant), non-temporary passwords. One public client `poc-frontend`: Authorization Code + PKCE (S256), redirect URIs `http://localhost:3000/*` and `http://localhost:4200/*`, web origins `+`. `directAccessGrantsEnabled: true` on the same client so developers can fetch a token with one `curl` (password grant) for API smoke tests — dev convenience, documented as not-for-production.

*Alternative considered:* separate confidential client for the backend — unnecessary; the backend only validates tokens, it never calls Keycloak as a client.

### D4: Backend — resource server with realm-role mapping, rules in one filter chain
Add `spring-boot-starter-oauth2-resource-server`. One `SecurityFilterChain` for `/api/**`: `POST /api/process-definitions/*/start` requires `ROLE_applicant`, `POST /api/tasks/*/complete` requires `ROLE_civil-servant`, everything else under `/api` requires authentication; CSRF off (stateless bearer). A `JwtAuthenticationConverter` maps `realm_access.roles[]` → `ROLE_<name>` authorities. 401/403 are returned as the existing structured JSON error shape (extend `ApiExceptionHandler` / entry-point + access-denied handler).

*Alternative considered:* `@PreAuthorize` method security — fine too, but URL-based rules keep the whole policy visible in one place for a POC this small.

### D5: Frontend — keycloak-angular (wrapping keycloak-js), login-required
Use `keycloak-angular`'s standalone providers (`provideKeycloak` + `includeBearerTokenInterceptor` matching `/api/**`) in `app.config.ts`, init with `onLoad: 'login-required'` and PKCE — the whole app is behind login, which matches "citizen portal" semantics and avoids mixed anonymous states. Keycloak URL comes from a small runtime config: same-origin default `http://localhost:8180` works for both :3000 and :4200 since Keycloak is called cross-origin directly (web origins `+` covers CORS). Header shows `preferred_username` + role label + logout button. Route-level `canActivate` guards by role: Services/start pages require `applicant`, Tasks pages require `civil-servant`, Processes visible to both. Nav links render only for permitted roles.

*Alternative considered:* raw `keycloak-js` with a hand-rolled interceptor — more code for the same result; keycloak-angular is the maintained idiomatic wrapper. Verify at implementation time that the latest keycloak-angular supports Angular 22 (it tracks Angular majors closely); fall back to raw `keycloak-js` if not.

### D6: nginx/proxy topology unchanged
Keycloak is NOT proxied behind `/api` or nginx — the SPA talks to `localhost:8180` directly (as in cib7, keeping the issuer URL stable). `frontend/proxy.conf.json` and `frontend/nginx.conf` stay as they are; compose adds only the new service + backend env vars.

## Risks / Trade-offs

- [Token in browser, dev-mode Keycloak, http-only] → Acceptable for a learning POC; explicitly documented as non-production in README/architecture docs.
- [keycloak-angular version may lag Angular 22] → Checked first task; fallback to plain keycloak-js documented in D5.
- [Backend JWKS fetch races Keycloak on cold start] → `depends_on: keycloak: condition: service_healthy` in compose; Spring's `NimbusJwtDecoder` fetches JWKS lazily on first request anyway, so local dev needs no ordering.
- [Password grant on a public client] → dev-only convenience for curl; called out in docs, trivially removable.
- [Existing e2e habits break (curl without token now 401)] → documented curl token recipe in README; Camunda UIs unaffected.

## Migration Plan

1. Ship compose + realm + backend + frontend in one change (the SPA is unusable against a secured API otherwise).
2. Fresh start recommended (`docker compose down` is enough — Keycloak realm import is idempotent on a fresh volume; no Camunda data migration involved).
3. Rollback = revert the commit; no persisted state depends on Keycloak (H2 Camunda data has no user linkage).

## Open Questions

- None blocking. (keycloak-angular ↔ Angular 22 compatibility is resolved as the first implementation task, with a stated fallback.)
