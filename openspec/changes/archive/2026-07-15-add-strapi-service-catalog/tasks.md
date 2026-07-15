## 1. Strapi CMS module (`cms/`)

- [x] 1.1 Scaffold Strapi v5 project in `cms/` (SQLite, pinned version, Node 22) and commit config + generated project files (no node_modules, no .tmp data)
- [x] 1.2 Define the `service` collection type (title, summary, instructions, whatYouNeed, expectedDuration, processDefinitionId unique) with draft & publish; commit the schema JSON
- [x] 1.3 Add bootstrap hook: grant public role `find`/`findOne` on `service`; seed + publish `vehicle-registration` and `business-registration` entries from a committed fixture when no entries exist (idempotent)
- [x] 1.4 Verify locally: fresh boot serves both published entries on `GET :1337/api/services` without credentials; drafts hidden; unauthenticated POST rejected

## 2. Docker integration

- [x] 2.1 Add `cms/Dockerfile` (build admin panel, production start) and a `strapi` service to `docker-compose.yml` (host port 1337, named volume for SQLite, required Strapi secrets via env)
- [x] 2.2 Verify `docker compose up -d strapi` on a fresh volume serves seeded content, and content survives restart (but not `down -v`)

## 3. Backend merge endpoint

- [x] 3.1 Add `StrapiClient` (Spring `RestClient`, env-overridable base URL defaulting to `http://localhost:1337`, ~2s timeout) fetching published `service` entries; map the v5 REST shape in one place
- [x] 3.2 Add `ServiceCatalogItem` record in `api/dto` with static `from(ProcessDefinition, StrapiService)` merging engine + content fields (content nullable)
- [x] 3.3 Add `GET /api/services` controller: latest deployed definitions joined to Strapi entries on `processDefinitionId`; engine-only items with a warning log when Strapi is down/missing; orphaned CMS entries dropped; any authenticated role may read
- [x] 3.4 Backend tests: merged item, definition-without-content, Strapi-down degradation, orphaned-entry exclusion (`./mvnw test`)

## 4. Frontend catalog UI

- [x] 4.1 Add `ServiceCatalogItem` model and `services()` call in `core/models.ts` / `core/api.service.ts`
- [x] 4.2 Update `services-page.ts` to consume `/api/services`: render CMS title + summary on cards, falling back to engine name/id when content is null
- [x] 4.3 Update `start-process-page.ts` to show `instructions` above the start form when present
- [x] 4.4 Frontend build + unit tests pass (`npx ng build`, `npx ng test`)

## 5. Docs and business spec

- [x] 5.1 Update `docs/architecture.md`: Strapi in the component diagram/flow, content ownership boundary (Camunda = executable artifacts, Strapi = editorial content, join key `processDefinitionId`), port map + 1337
- [x] 5.2 Update README: Strapi admin first-boot registration, dev loop (`docker compose up -d orchestration connectors keycloak strapi`), editing catalog content
- [x] 5.3 Update CLAUDE.md commands/gotchas (dev loop service set, Strapi port, `down -v` also wipes CMS content)
- [x] 5.4 Add catalog content fields to `docs/business/services/<service>/README.md` for both services (source-of-truth chain: business spec ↔ seeded content)

## 6. End-to-end verification

- [x] 6.1 Full stack: `docker compose up --build` on a clean slate → Services page shows seeded editorial content; `bart` starts a process; `homer` completes the task
- [x] 6.2 Degradation: stop the strapi container → Services page still lists startable services with engine names
- [x] 6.3 Editorial loop: edit a summary in the Strapi admin, publish, reload the Services page → new text appears with no redeploy
