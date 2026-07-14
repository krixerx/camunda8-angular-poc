# frontend-app Specification (delta)

## MODIFIED Requirements

### Requirement: Angular SPA talks only to the backend
The `frontend/` module SHALL be an Angular 22 application (standalone components, strict TypeScript) that calls only the backend `/api/**` endpoints for business data — never the Camunda API directly. The single permitted exception is the Keycloak server at `http://localhost:8180`, which the SPA calls directly for OIDC login and token operations. In development, `ng serve` SHALL proxy `/api` to `http://localhost:8085`.

#### Scenario: Dev proxy works
- **WHEN** the developer runs `npm start` in `frontend/` with the backend running
- **THEN** the app at `http://localhost:4200` loads data through the proxied `/api` endpoints

#### Scenario: No direct Camunda calls
- **WHEN** the SPA is exercised end-to-end
- **THEN** every outgoing request targets either `/api/**` (same-origin) or the Keycloak realm endpoints — none target the Camunda cluster

## ADDED Requirements

### Requirement: Login via Keycloak
The SPA SHALL initialize Keycloak (OIDC Authorization Code + PKCE, client `poc-frontend`) at bootstrap with login-required semantics: an unauthenticated visitor is redirected to the Keycloak login page and returns to the app after authenticating. The app SHALL display the logged-in user's username and role, and offer a logout action that ends the Keycloak session and returns to the login screen.

#### Scenario: Unauthenticated visitor is redirected to login
- **WHEN** a user with no session opens `http://localhost:3000`
- **THEN** the browser is redirected to the Keycloak login page, and after logging in as `bart`/`bart` lands back in the SPA

#### Scenario: Logout ends the session
- **WHEN** the logged-in user clicks logout
- **THEN** the Keycloak session is terminated and revisiting the app requires logging in again

### Requirement: Bearer token attached to API calls
Every request the SPA sends to `/api/**` SHALL carry the user's access token as an `Authorization: Bearer` header (HTTP interceptor); the token SHALL be refreshed automatically while the session is active.

#### Scenario: API call carries token
- **WHEN** the logged-in SPA loads the Services page
- **THEN** the `GET /api/process-definitions` request includes a valid `Authorization: Bearer` header and succeeds

### Requirement: Role-based UI
Navigation and routes SHALL be gated by realm role: Services and start-process pages require `applicant`; Tasks and task-detail pages require `civil-servant`; the Processes page is available to any authenticated user. Links to pages the user's role does not permit SHALL not be rendered, and direct navigation to a forbidden route SHALL be blocked by a route guard.

#### Scenario: bart sees the applicant UI
- **WHEN** `bart` logs in
- **THEN** the nav shows Services and Processes (not Tasks), and navigating to `/tasks` by URL is blocked

#### Scenario: homer sees the civil-servant UI
- **WHEN** `homer` logs in
- **THEN** the nav shows Tasks and Processes (not Services), and homer can open and complete a review task
