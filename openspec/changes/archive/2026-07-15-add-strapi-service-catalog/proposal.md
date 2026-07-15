## Why

The Services page renders raw engine data (`processDefinitionId · v3`) — there is no citizen-facing editorial content, and any wording change requires a developer commit and redeploy. We need to prove Strapi (headless CMS) in this POC, and service catalog content is the natural fit: it changes far more often than the BPMN, is owned by content editors rather than developers, and demonstrates a clean boundary between orchestration (Camunda) and editorial content (Strapi).

## What Changes

- Add a Strapi v5 CMS (`cms/`, SQLite storage) to the stack with a `service` content type holding citizen-facing catalog content (title, summary, instructions, what-you-need, expected duration), joined to Camunda by `processDefinitionId`.
- Seed content for both existing services (`vehicle-registration`, `business-registration`) so a fresh `docker compose up` starts populated.
- Backend gains a `StrapiClient` and a new `GET /api/services` endpoint that merges deployed process definitions (Camunda) with published catalog content (Strapi) into one `ServiceCatalogItem` DTO; content fields are null-safe so the catalog degrades gracefully when Strapi is down or an entry is missing.
- Frontend Services page consumes `/api/services` and renders the editorial title/summary on cards; the start page shows `instructions` above the start form. Falls back to engine data when no CMS content exists.
- Docker compose gains a `strapi` service; dev loop docs cover running Strapi alongside the cluster and Keycloak.
- `docs/architecture.md` documents the content boundary: Camunda owns executable artifacts (BPMN/DMN/forms, deployment-versioned); Strapi owns editorial content; joined by `processDefinitionId`.

## Capabilities

### New Capabilities
- `cms-service-catalog`: Strapi CMS hosting the `service` content type — content model, draft & publish, read-only API token access, seed content, and the Camunda/Strapi content-ownership boundary.

### Modified Capabilities
- `backend-api`: New requirement — `GET /api/services` merges Camunda process definitions with Strapi catalog content; graceful degradation when Strapi is unavailable.
- `frontend-app`: "Services page starts processes" requirement changes — the page consumes `/api/services` and renders editorial content, with fallback to engine data.
- `deployment`: Full-stack compose gains the Strapi service (with seeded content on first boot); dev loop includes Strapi in the Dockerized infrastructure set.

## Impact

- **New code**: `cms/` (Strapi project + seed data), `com.poc.backend.cms` (StrapiClient + config), `ServiceCatalogItem` DTO, `ServicesController`.
- **Changed code**: `frontend/src/app/pages/services-page.ts`, `start-process-page.ts`, `core/api.service.ts`, `core/models.ts`; `docker-compose.yml`; nginx/dev-proxy untouched (frontend still talks only to `/api`).
- **Dependencies**: Strapi v5 (Node 22 compatible), SQLite (bundled); backend needs no new Maven deps (Spring `RestClient`).
- **Ports**: Strapi admin/API on host `1337` (new entry in the documented port map).
- **Docs**: `docs/architecture.md` (boundary + port map), README (Strapi admin credentials, dev loop).
