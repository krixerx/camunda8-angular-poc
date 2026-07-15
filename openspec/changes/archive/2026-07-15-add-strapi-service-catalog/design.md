## Context

The Services page (`frontend/src/app/pages/services-page.ts`) lists whatever `GET /api/process-definitions` returns and renders raw engine data. There is no editorial layer: no citizen-friendly descriptions, instructions, or fee/duration info, and any copy change requires a code commit. The POC needs to prove Strapi, and this is the highest-value slot for it: catalog content changes on an editorial cadence, not a deployment cadence.

The existing architecture is a strict facade: browser → Angular → `/api` (Spring Boot) → Camunda. Keycloak is the single permitted browser-side exception. This change must not erode that shape.

## Goals / Non-Goals

**Goals:**
- Prove Strapi end-to-end: content modeling, draft & publish, admin editing, API consumption, Docker deployment, content seeding.
- Editorial catalog content per service, joined to Camunda process definitions by `processDefinitionId`.
- Keep the facade intact: the browser keeps talking only to `/api` (plus Keycloak); the backend merges Camunda + Strapi.
- Graceful degradation: catalog works (engine data only) when Strapi is down or has no matching entry.
- Document the ownership boundary: Camunda owns executable artifacts; Strapi owns editorial content.

**Non-Goals:**
- No form schemas, BPMN, or DMN in Strapi — those stay deployment-versioned in Camunda (`bindingType="deployment"` governance).
- No process/business data in Strapi — it is not an operational database.
- No i18n in this change (natural follow-up once the catalog exists).
- No Strapi editing UI integration in the Angular app — editors use the Strapi admin panel.
- No Keycloak SSO for the Strapi admin panel (Strapi's own admin login is fine for a POC).

## Decisions

**Strapi v5 + SQLite, new top-level `cms/` module.** SQLite keeps the container self-contained (mirrors the cluster's H2 choice — no extra database service). The Strapi project (config, `src/api/service/` content-type schema JSON, bootstrap code) is committed; generated content-type schemas live in the repo so the content model is code-reviewed like everything else. Alternative considered: Postgres — rejected as a second stateful service with no POC payoff.

**Join key is `processDefinitionId`.** A plain string field (unique) on the `service` content type matching the BPMN process id (`vehicle-registration`, `business-registration`). No webhook/sync between Strapi and Camunda — the backend joins at read time. Alternative considered: storing the numeric `processDefinitionKey` — rejected because keys change every deployment; the BPMN id is the stable identity.

**Backend facade merges; new sibling endpoint `GET /api/services`.** A `StrapiClient` (Spring `RestClient`, base URL env-overridable: `http://localhost:1337` dev, `http://strapi:1337` in Docker) fetches published `service` entries; `ServiceCatalogItem` (record in `api/dto`, static `from()` per project convention) merges a Camunda definition with its optional Strapi entry. Content fields are nullable; a Strapi outage or missing entry yields engine-data-only items (log a warning, never fail the endpoint). The existing `/api/process-definitions` endpoint stays untouched — it remains the raw engine view. Alternatives considered: Angular calling Strapi directly (breaks the facade rule and adds a second browser origin/proxy); extending `/api/process-definitions` in place (muddies the "raw engine" endpoint and its existing spec scenarios).

**Read access: Strapi public role gets `find`/`findOne` on `service`, granted programmatically in the bootstrap hook.** Catalog content is public-by-nature (it is what a government portal shows before login), and the deployment spec requires zero manual steps after `docker compose up`. Alternative considered: a read-only API token for the backend — rejected because Strapi tokens are generated hashed at creation time; seeding one deterministically is fiddly, and creating one manually violates the no-manual-steps requirement. Writes and the admin panel remain protected by Strapi's own auth.

**Seeding via Strapi bootstrap hook, mirroring the Keycloak realm-import pattern.** On startup, if no `service` entries exist, the bootstrap function (in `cms/src/index`) creates and publishes the two seed entries from a committed JSON fixture. Idempotent (skips when content exists), survives `docker compose down` (volume) and repopulates after `down -v`. Alternative considered: `strapi export`/`import` tarball — rejected: encrypted-by-default artifacts and a separate import step fit CI poorly; a bootstrap function is plain reviewable code.

**Ports and compose.** Strapi publishes host port `1337` (admin panel + API; dev-loop backend also reaches it there). Compose service `strapi` joins the dev-loop infra set (`docker compose up -d orchestration connectors keycloak strapi`). The backend does not hard-depend on Strapi health (graceful degradation makes an ordering dependency unnecessary).

**First-boot admin user.** Strapi requires an admin registration on first visit to `:1337/admin`. This is acceptable for editing content, and the seeded content + public read means the end-to-end flow works without it. Documented in README.

## Risks / Trade-offs

- [Strapi v5 API response shape changes between minors] → Pin the Strapi version in `cms/package.json`; `StrapiClient` maps the documented v5 REST shape (`data[].attributes` flattened in v5) in one place.
- [Public read on the content type exposes Strapi's REST API on host port 1337] → Acceptable for a POC and consistent with the content being public; only `find`/`findOne` on `service` are granted. Noted in docs as a production-hardening point (API token or network-internal-only).
- [Backend adds a synchronous Strapi call to the catalog request path] → Short timeout (~2s) on `StrapiClient`; on timeout/error return engine-only items so Camunda availability alone determines catalog availability.
- [Content/engine drift: Strapi entry exists for a process that is no longer deployed] → The merge is driven by deployed definitions; orphaned CMS entries are simply not shown. The inverse (deployed process without content) renders the fallback card.
- [Node version mismatch] → Strapi v5 supports Node 22; the repo already mandates Node ≥ 22.22.3, and the Docker image pins its own Node.

## Migration Plan

Purely additive: new container, new endpoint, frontend switches the Services page to `/api/services`. No data migration; rollback = remove the compose service and revert the frontend to `/api/process-definitions`. `docker compose down -v` remains the clean-slate path (now also wipes Strapi's SQLite volume).

## Open Questions

- None blocking. (i18n and step-level guidance content are deferred follow-ups, noted as non-goals.)
