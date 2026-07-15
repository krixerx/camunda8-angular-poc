## ADDED Requirements

### Requirement: Service catalog endpoint merges Camunda and Strapi
The backend SHALL expose `GET /api/services` returning one `ServiceCatalogItem` per latest deployed process definition, merging engine data (id, name, version, key) with the matching published Strapi `service` entry (title, summary, instructions, whatYouNeed, expectedDuration) joined on `processDefinitionId`. Strapi SHALL be read via a dedicated client with an env-overridable base URL (localhost in dev, service DNS in Docker) and a short request timeout. Content fields SHALL be null when no Strapi entry matches. The endpoint SHALL require a valid bearer token (any realm role), consistent with other read endpoints. `ServiceCatalogItem` SHALL follow the project DTO convention: a separate record in the `dto` package with a static `from()` mapper.

#### Scenario: Merged catalog item
- **WHEN** the frontend requests `GET /api/services` while `vehicle-registration` is deployed and has a published Strapi entry
- **THEN** the response item for `vehicle-registration` carries both the engine fields and the editorial content fields

#### Scenario: Definition without CMS content
- **WHEN** a process definition is deployed with no matching published Strapi entry
- **THEN** the catalog item is still returned with engine fields populated and content fields null

#### Scenario: Strapi unavailable degrades gracefully
- **WHEN** the frontend requests `GET /api/services` while Strapi is down or times out
- **THEN** the backend returns HTTP 200 with engine-only items (all content fields null) and logs a warning — it does not return an error

#### Scenario: Orphaned CMS entry not shown
- **WHEN** a published Strapi entry references a `processDefinitionId` that has no deployed process definition
- **THEN** no catalog item is produced for it
