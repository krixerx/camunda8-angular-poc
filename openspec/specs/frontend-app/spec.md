# frontend-app Specification

## Purpose
TBD - created by archiving change bootstrap-poc. Update Purpose after archive.
## Requirements
### Requirement: Angular SPA talks only to the backend
The `frontend/` module SHALL be an Angular 22 application (standalone components, strict TypeScript) that calls only the backend `/api/**` endpoints for business data — never the Camunda API directly. The single permitted exception is the Keycloak server at `http://localhost:8180`, which the SPA calls directly for OIDC login and token operations. In development, `ng serve` SHALL proxy `/api` to `http://localhost:8085`.

#### Scenario: Dev proxy works
- **WHEN** the developer runs `npm start` in `frontend/` with the backend running
- **THEN** the app at `http://localhost:4200` loads data through the proxied `/api` endpoints

#### Scenario: No direct Camunda calls
- **WHEN** the SPA is exercised end-to-end
- **THEN** every outgoing request targets either `/api/**` (same-origin) or the Keycloak realm endpoints — none target the Camunda cluster

### Requirement: Services page starts processes
A Services page SHALL list the service catalog from `GET /api/services`, rendering the editorial title and summary from CMS content on each service card; when a catalog item has no CMS content, the card SHALL fall back to the engine data (definition name and id). Starting a service SHALL render its start form (if any) with form-js and submit the values to `POST /api/process-definitions/{key}/start`; when the catalog item carries `instructions`, the start page SHALL display them above the start form.

#### Scenario: Start a process from the catalog
- **WHEN** the user opens the Services page, chooses "Vehicle registration", fills the start form, and submits
- **THEN** a new process instance is created and the user is navigated to a confirmation (or the created task)

#### Scenario: Card shows editorial content
- **WHEN** the Services page loads and `vehicle-registration` has published CMS content
- **THEN** its card shows the CMS title and summary instead of the raw engine name

#### Scenario: Card falls back to engine data
- **WHEN** the Services page loads and a deployed definition has no CMS content (or Strapi is down)
- **THEN** its card still renders with the engine definition name and remains startable

#### Scenario: Instructions shown before the start form
- **WHEN** the user opens the start page for a service whose catalog item carries `instructions`
- **THEN** the instructions are rendered above the start form

### Requirement: Tasks page lists and opens user tasks
A Tasks page SHALL list open user tasks from `GET /api/tasks`. Opening a task SHALL show a Task detail view that renders the task's Camunda Form schema with `@bpmn-io/form-js-viewer`, pre-filled from current variables, and completes the task via `POST /api/tasks/{key}/complete`.

#### Scenario: Complete a review task
- **WHEN** the user opens a "Review registration" task, edits the form, and submits
- **THEN** the task completes, the user returns to the Tasks page, and the task is gone from the list

### Requirement: form-js wrapper component
Camunda Form rendering SHALL be encapsulated in a single reusable Angular component that instantiates the form-js viewer on an element ref, imports the required form-js CSS, emits submit events with `{ data, errors }`, blocks submission when validation errors exist, and destroys the viewer on component teardown.

#### Scenario: Client-side validation
- **WHEN** the user submits a form with a required field empty
- **THEN** form-js shows the validation error and no request is sent to the backend

### Requirement: Processes page shows instances
A Processes page SHALL list process instances from `GET /api/process-instances` with their state (active/completed) and start date.

#### Scenario: Completed instance visible
- **WHEN** a process instance finishes and the user opens the Processes page
- **THEN** the instance is listed with state "completed"

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

