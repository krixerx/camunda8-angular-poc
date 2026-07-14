# authentication Specification (delta)

## ADDED Requirements

### Requirement: Keycloak runs as a separate microservice
The stack SHALL include a `keycloak` service in `docker-compose.yml` (`quay.io/keycloak/keycloak:26.x`) running in dev mode with realm auto-import (`start-dev --import-realm`), published on host port **8180**, with a healthcheck on the Keycloak management port so dependent services can wait for readiness. The realm definition SHALL live in the repository (`docker/keycloak/realm-export.json`) so a clean checkout needs no manual identity setup.

#### Scenario: Realm imported on cold start
- **WHEN** the developer runs `docker compose up` on a clean checkout
- **THEN** Keycloak starts on `http://localhost:8180` with the `camunda-poc` realm, its roles, users, and client already present

#### Scenario: Admin console reachable
- **WHEN** the developer opens `http://localhost:8180` and logs in with the documented admin bootstrap credentials
- **THEN** the `camunda-poc` realm is visible and editable

### Requirement: Realm defines demo users and roles
The `camunda-poc` realm SHALL define realm roles `applicant` and `civil-servant`, and two demo users with non-temporary passwords: `bart` (password `bart`) holding role `applicant`, and `homer` (password `homer`) holding role `civil-servant`.

#### Scenario: bart authenticates as applicant
- **WHEN** `bart` logs in with password `bart`
- **THEN** authentication succeeds and the issued access token contains `applicant` in `realm_access.roles`

#### Scenario: homer authenticates as civil servant
- **WHEN** `homer` logs in with password `homer`
- **THEN** authentication succeeds and the issued access token contains `civil-servant` in `realm_access.roles`

### Requirement: Public SPA client with PKCE
The realm SHALL define a public OIDC client `poc-frontend` configured for Authorization Code flow with PKCE (S256), with redirect URIs covering both frontend origins (`http://localhost:3000/*`, `http://localhost:4200/*`) and matching web origins for CORS. The client SHALL also allow direct access grants so developers can obtain a token via `curl` (password grant) for API smoke testing; this convenience SHALL be documented as dev-only.

#### Scenario: SPA login round-trip from either origin
- **WHEN** the SPA at `http://localhost:3000` (Docker) or `http://localhost:4200` (dev) redirects to Keycloak and the user logs in
- **THEN** Keycloak redirects back to the originating URL and the SPA obtains valid tokens

#### Scenario: Developer fetches a token with curl
- **WHEN** the developer posts `grant_type=password&client_id=poc-frontend&username=bart&password=bart` to the realm token endpoint
- **THEN** the response contains a usable `access_token`

### Requirement: Stable issuer URL across browser and containers
Keycloak SHALL be pinned to the canonical issuer `http://localhost:8180` (via `KC_HOSTNAME_URL`) so that every issued token carries `iss=http://localhost:8180/realms/camunda-poc` regardless of whether it is validated by the browser, a local process, or a container. Token consumers inside the Docker network SHALL fetch keys via a network-reachable URL while validating the issuer claim against the canonical public URL.

#### Scenario: Dockerized backend accepts a browser-obtained token
- **WHEN** the SPA obtains a token from `localhost:8180` and sends it to the backend container
- **THEN** the backend validates signature (via internal JWKS URL) and issuer (string match on the canonical URL) successfully

### Requirement: Role semantics
The role `applicant` SHALL represent citizens who submit applications (start process instances). The role `civil-servant` SHALL represent officials who review submissions (complete user tasks). These semantics SHALL be enforced by the backend API and reflected in the frontend UI, and SHALL be documented in the business-service docs (`docs/business/services/*/README.md` "Roles / authorization" sections).

#### Scenario: Business docs state the role model
- **WHEN** a reader opens a business-service README after this change
- **THEN** the "Roles / authorization" section names which role starts the process and which role completes each user task
