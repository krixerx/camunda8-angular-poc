## Context

Everything the citizen sees is English-only: the Strapi-backed catalog (`GET /api/services`), the Camunda Form schemas served through the facade, and the Angular app chrome. The sibling `cib7-react-poc` proves EN/AR with a very different shape — bundled i18next JSON, hand-built React forms, RTL via `document.dir` + CSS logical properties, locale in `localStorage` (`cib7.lang`). This project must reach the same user-visible result while keeping its own architecture honest: forms stay real deployed Camunda Forms rendered by form-js, editorial content stays in Strapi, the browser talks only to `/api` (plus Keycloak).

Strapi 5 ships the i18n feature in core (locale-per-document with non-localized field support). The backend facade already intercepts every form schema on its way to the browser, which is the natural seam for translation overlay.

## Goals / Non-Goals

**Goals:**
- English + Arabic across catalog, start/task forms, and app chrome; Arabic renders right-to-left.
- Editors own translations (Strapi, editorial cadence); developers own chrome strings and authored English form labels (code cadence).
- Same degradation contract as the catalog: missing translation or Strapi outage ⇒ authored English, never an error.
- Existing English behavior unchanged when no locale is requested.

**Non-Goals:**
- No third language, no locale negotiation via `Accept-Language`, no per-user locale in Keycloak (localStorage only, like cib7).
- No translation of Camunda-internal UIs (Operate/Tasklist) or the Strapi admin panel.
- No translation of engine data (process/task names from BPMN) — out of scope; the catalog title already shadows the process name where it matters. (cib7's `backendNames.ts` lookup-map trick is a possible follow-up.)
- No RTL-specific redesign — layout correctness via logical properties, not a second stylesheet.
- No localized number/date formatting beyond what `Intl` gives for free.

## Decisions

**Strapi i18n on `service`, `en` default + `ar`.** Content fields (`title`, `summary`, `instructions`, `whatYouNeed`, `expectedDuration`) become localized; `processDefinitionId` is marked non-localized so every locale version of a document shares the join key. Locales are configured in code (`cms/config/plugins.js` / schema JSON `pluginOptions`), committed like everything else. Alternative considered: separate `service-ar` collection — rejected, Strapi's document-locale model exists for exactly this.

**Form translations are data, not schema.** New collection `form-translation`: `formId` (string, the kebab-case Camunda form id, non-localized), plus a localized JSON field `strings` holding `{ <fieldKey>: { label, description, placeholder, values: {<optionValue>: <optionLabel>} }, <textViewId>: { text } }`. One document per form; Arabic is the only seeded localization (English lives in the authored `.form` files as fallback, so there is exactly one source of truth per string per language). Alternative considered: per-locale `.form` files deployed to Camunda — rejected: combinatorial redeploys, version skew across locales, contradicts the editorial-cadence goal.

**Overlay in the backend facade, presentational strings only.** A `FormTranslator` walks the schema returned by Camunda and, when a translation exists for (formId, locale), replaces `label`, `description`, `placeholder`, select/radio `values[].label`, and text-view `text`. It never touches `key`, `type`, `validate`, `conditional`, or any FEEL expression — this constraint is the ownership boundary in code and gets its own tests. Form id for the lookup comes from the schema's own `id` field (all four `.form` files carry kebab-case ids). Alternatives considered: frontend-side overlay (breaks the thin-client facade principle, duplicates fetch logic across pages); FEEL-expression labels reading injected translation variables (pollutes schemas with i18n plumbing, unusable for editors).

**Locale is an explicit query param, `en` is implicit default.** `GET /api/services?locale=ar`, `GET /api/process-definitions/{key}/form?locale=ar`, `GET /api/tasks/{key}?locale=ar`. Omitted or `en` ⇒ today's behavior byte-for-byte (no Strapi form-translation fetch at all for `en`). Only `ar` is accepted besides `en`; unknown values fall back to `en` rather than erroring (a POC forgiveness choice). Alternative considered: `Accept-Language` header — rejected as implicit magic; a visible param matches the explicit switcher UX.

**Catalog locale fallback happens in the backend.** For `locale=ar` the backend requests Arabic entries from Strapi; services whose Arabic localization is missing fall back to that service's English entry (second fetch of the default locale, merged by `processDefinitionId`). Rationale: the frontend keeps exactly one code path, and "partially translated" content degrades to English per-service instead of per-page.

**Frontend mirrors cib7's mechanics, not its library.** A tiny `LanguageService` (Angular signal): persists to `localStorage` key `c8poc.lang`, sets `document.documentElement.lang` and `dir` (`rtl` for `ar`), and exposes the current locale to `ApiService` (which appends `?locale=` when not `en`). App chrome strings come from a hand-rolled `Record<key, {en, ar}>` dictionary in `core/i18n.ts` — a dozen strings do not justify ngx-translate, and chrome is developer-owned by design. Global stylesheet audited to logical properties (`margin-inline-*`, `text-align: start`); form-js inherits the document direction. Alternatives considered: ngx-translate (a dependency for ~15 strings), Angular built-in `$localize` (compile-time, two builds, no runtime switch — wrong tool for a switcher demo).

**Seeding.** The bootstrap hook seeds, idempotently as today: `service` in `en` and `ar` (new fixture `seed-services.ar.json`), `form-translation` documents with `ar` strings for the four forms (`seed-form-translations.json`). Public role gets `find`/`findOne` on `form-translation` exactly like `service`.

## Risks / Trade-offs

- [Strapi 5 i18n API details (locale param shape, non-localized field behavior) differ from docs] → All Strapi access is already funneled through `StrapiClient`; verify against the running container early (task 1.x) before backend work builds on it.
- [`strings` as a free-form JSON field has no schema validation in Strapi admin — an editor can break the shape] → Overlay treats every lookup as optional (`missing ⇒ keep authored string`); malformed JSON degrades to English, and the seed fixture documents the expected shape.
- [Form/version drift: a redeployed form adds a field with no Arabic translation] → By-design fallback to the authored English label; no hard failure. Documented as the editorial follow-up workflow.
- [RTL regressions in existing CSS (physical `margin-left` etc.)] → One audit pass over `styles.css` + the two page components; acceptance includes an Arabic screenshot check of catalog, start form, and task form.
- [Two Strapi fetches per Arabic catalog request (ar + en fallback)] → Bounded by the existing 2s client timeout; POC-acceptable. A cache is a deliberate non-feature (invalidation adds more complexity than two 10-entry fetches cost).
- [Arabic seed copy is developer-written, not translator-verified] → Acceptable for a POC proof; flagged in the business-spec tables so a reviewer knows its provenance.

## Migration Plan

Purely additive. Existing volumes: the i18n schema change deploys with the new Strapi image; already-seeded English documents stay untouched, and the bootstrap hook seeds only what is missing (`ar` localizations, `form-translation` entries) so both fresh and existing volumes converge. Rollback = revert the change; English behavior never depended on the new code paths. `docker compose down -v` remains the clean-slate path.

## Open Questions

- None blocking. (Keycloak-locale sync and engine-name translation are noted follow-ups; Arabic copy should get a native-speaker review before any real demo.)
