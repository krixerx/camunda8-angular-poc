# cms-service-catalog Specification

## Purpose
TBD - created by syncing change add-strapi-service-catalog. Update Purpose after archive.
## Requirements
### Requirement: Strapi hosts the service content type
A Strapi v5 application (`cms/`, SQLite storage, version pinned) SHALL define a `service` collection type with draft & publish enabled and the fields: `title` (string, required), `summary` (text), `instructions` (rich text), `whatYouNeed` (rich text), `expectedDuration` (string), and `processDefinitionId` (string, required, unique) — the join key matching the BPMN process id of a deployed Camunda process definition. The content-type schema SHALL be committed to the repository.

#### Scenario: Content model present after boot
- **WHEN** Strapi starts and an editor opens the admin panel content manager
- **THEN** the `service` collection type exists with the specified fields and draft & publish enabled

#### Scenario: Editor updates catalog copy without a deployment
- **WHEN** an editor changes the `summary` of a published `service` entry in the Strapi admin panel and saves + publishes
- **THEN** the updated text is served by the Strapi API without rebuilding or restarting any other component

### Requirement: Published service content is readable without authentication
The Strapi bootstrap hook SHALL grant the public role `find` and `findOne` permissions on the `service` content type, so the backend can read published entries without credentials. All write operations and the admin panel SHALL remain protected by Strapi's own authentication. Only published entries SHALL be returned by the public API; drafts SHALL NOT be visible.

#### Scenario: Backend reads published entries
- **WHEN** a client sends `GET /api/services` to the Strapi API (port 1337) without credentials
- **THEN** Strapi returns HTTP 200 with the published `service` entries

#### Scenario: Drafts are not exposed
- **WHEN** an editor saves a `service` entry as draft without publishing and a client queries the public API
- **THEN** the draft entry is absent from the response

#### Scenario: Unauthenticated writes rejected
- **WHEN** a client sends `POST /api/services` to the Strapi API without credentials
- **THEN** Strapi responds with an error status (401/403/405) and no entry is created

### Requirement: Seed content on first boot
The Strapi bootstrap hook SHALL create and publish `service` entries for `vehicle-registration` and `business-registration` from a committed fixture when no `service` entries exist. Seeding SHALL be idempotent: it runs on an empty database (fresh volume) and is skipped when content already exists, so editor changes survive restarts.

#### Scenario: Fresh stack starts populated
- **WHEN** the stack starts on a fresh volume (`docker compose up` after `down -v`)
- **THEN** the Strapi API serves published entries for both `vehicle-registration` and `business-registration` without any manual step

#### Scenario: Editor changes survive restart
- **WHEN** an editor edits a seeded entry and the Strapi container is restarted (volume retained)
- **THEN** the edited content is served and the seed fixture does not overwrite it

### Requirement: Content ownership boundary is documented
`docs/architecture.md` SHALL document the ownership boundary: Camunda owns executable artifacts (BPMN, DMN, form schemas — versioned via deployment), Strapi owns editorial catalog content, and the two are joined by `processDefinitionId`. Executable artifacts SHALL NOT be stored in or served from Strapi.

#### Scenario: Boundary documented
- **WHEN** a developer reads `docs/architecture.md`
- **THEN** it states which artifact types belong to Camunda, which content belongs to Strapi, and names `processDefinitionId` as the join key
