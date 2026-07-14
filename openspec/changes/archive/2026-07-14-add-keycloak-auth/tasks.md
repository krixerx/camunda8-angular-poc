# Tasks: add-keycloak-auth

## 1. Keycloak service + realm

- [x] 1.1 Create `docker/keycloak/realm-export.json`: realm `camunda-poc`, realm roles `applicant` and `civil-servant`, users `bart`/`bart` (applicant) and `homer`/`homer` (civil-servant, non-temporary passwords), public client `poc-frontend` (Auth Code + PKCE S256, redirect URIs `http://localhost:3000/*` + `http://localhost:4200/*`, web origins `+`, direct access grants enabled)
- [x] 1.2 Add `keycloak` service to `docker-compose.yml`: `quay.io/keycloak/keycloak:26.x`, `start-dev --import-realm`, realm JSON mounted read-only, `KC_HOSTNAME_URL=http://localhost:8180`, `KC_HEALTH_ENABLED=true`, ports `8180:8080`, TCP healthcheck on management port 9000, admin bootstrap `admin`/`admin`
- [x] 1.3 Verify cold start: `docker compose up -d keycloak`, realm visible at `http://localhost:8180`, both users can log in, `curl` password grant against `poc-frontend` returns an access token with the expected `realm_access.roles` and `iss=http://localhost:8180/realms/camunda-poc`

## 2. Backend resource server

- [x] 2.1 Add `spring-boot-starter-oauth2-resource-server` to `backend/pom.xml`; add JWKS/issuer properties to `application.yaml` (defaults for local dev: `http://localhost:8180/...`; env-overridable like `CAMUNDA_REST_ADDRESS`) and set the Docker override (`http://keycloak:8080/...` JWKS) in compose backend env
- [x] 2.2 Create `com.poc.backend.security.SecurityConfig`: single `/api/**` filter chain, stateless + CSRF off, `POST /api/process-definitions/*/start` → `ROLE_applicant`, `POST /api/tasks/*/complete` → `ROLE_civil-servant`, other `/api` requests authenticated; `JwtAuthenticationConverter` mapping `realm_access.roles[]` → `ROLE_*`; custom `JwtDecoder` bean using the internal JWKS URL with issuer validation against the canonical public URL
- [x] 2.3 Return 401/403 as the existing structured JSON error shape (`ApiError`): authentication entry point + access denied handler wired into the chain
- [x] 2.4 Add `depends_on: keycloak: condition: service_healthy` to the backend service in compose
- [x] 2.5 Verify with curl: no token → 401 JSON; bart token on `GET /api/tasks` → 200; homer token on start → 403 JSON; bart token on start → 200; homer token on complete → 200 (`./mvnw test` + manual smoke)

## 3. Frontend login + role-based UI

- [x] 3.1 Add `keycloak-angular` + `keycloak-js` to `frontend/package.json` (verify Angular 22 support; fall back to raw `keycloak-js` per design D5 if incompatible)
- [x] 3.2 Wire auth in `app.config.ts`: `provideKeycloak` (url `http://localhost:8180`, realm `camunda-poc`, client `poc-frontend`), `onLoad: 'login-required'`, PKCE S256, bearer interceptor scoped to `/api/**` with automatic token refresh
- [x] 3.3 Add an auth service/helper exposing username and roles from the token; header UI in `app.ts` showing `preferred_username`, role label, and a logout button
- [x] 3.4 Role-based routing: `canActivate` guards — Services + start-process routes require `applicant`, Tasks + task-detail routes require `civil-servant`, Processes any authenticated user; nav links render only for permitted roles; forbidden direct navigation redirects to the user's home page
- [x] 3.5 `npx ng build` + `npx ng test` pass (adjust `app.spec.ts` for the new bootstrap/providers)

## 4. End-to-end verification

- [x] 4.1 Full Docker mode: `docker compose up --build -d`, open `http://localhost:3000` → Keycloak login appears; as `bart` start a vehicle registration; logout; as `homer` complete the review task; verify in Operate
- [x] 4.2 Dev loop mode: `docker compose up -d orchestration connectors keycloak`, local backend + `npm start`, repeat the bart/homer round-trip at `http://localhost:4200`
- [x] 4.3 Negative checks: bart cannot see/open Tasks (nav + direct URL), homer cannot see/open Services; API returns 401 without token and 403 on wrong-role writes

## 5. Documentation

- [x] 5.1 Update `docs/business/services/vehicle-registration/README.md` and `business-registration/README.md` "Roles / authorization" sections with the applicant/civil-servant model (same commit as the code per convention)
- [x] 5.2 Update `docs/architecture.md`: Keycloak in the component diagram/description, port 8180 in the port map, token flow (SPA → Keycloak direct; bearer on `/api`; issuer/JWKS split for Docker)
- [x] 5.3 Update root `README.md`: login credentials (bart/bart, homer/homer, admin/admin), curl token recipe for API smoke tests (dev-only note)
- [x] 5.4 Update `CLAUDE.md`: new service in commands section, Keycloak gotchas (canonical issuer, 8180 port, realm re-import needs fresh volume)
