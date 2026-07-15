## Why

The portal is English-only, while the sibling `cib7-react-poc` demo already ships English + Arabic pages. This change proves the same on the Camunda 8 stack — with the harder twist this stack demands: cib7 translates hand-built React forms with bundled i18next JSON, whereas here the user-facing forms are real deployed **Camunda Forms** and editorial content lives in **Strapi**. Arabic is the right proof language because it forces right-to-left layout, not just string swapping.

## What Changes

- Enable Strapi's built-in i18n on the `service` collection: locales `en` (default) + `ar`; content fields localized, `processDefinitionId` stays non-localized (it is the join key). Seed both locales.
- New Strapi collection `form-translation`: per form id + locale, a map of presentational strings (field labels, descriptions, placeholders, select option labels, text views) for the deployed Camunda Forms. Seeded for all four forms in `ar` (English stays authored in the `.form` files as the fallback).
- Backend: `GET /api/services` accepts `?locale=`; the two form-schema endpoints (`/api/process-definitions/{key}/form`, `/api/tasks/{key}`) accept `?locale=` and overlay Strapi form translations onto the schema at read time — presentational strings only, never keys, types, validators, or FEEL expressions. Missing translation or Strapi down ⇒ authored English, never an error.
- Frontend: EN⇄AR language switcher (persisted in `localStorage`, mirroring cib7's `cib7.lang`), `document.documentElement` `lang`/`dir` sync (`dir="rtl"` for Arabic), CSS logical properties for direction-safe layout, and a small developer-owned dictionary for app chrome strings (nav, buttons, empty states) — chrome is code-cadence, so it does not go in the CMS.

## Capabilities

### New Capabilities
- `cms-form-translations`: Strapi hosts per-locale presentational-string translations for deployed Camunda Forms; public read, seeded, presentational-only contract.

### Modified Capabilities
- `cms-service-catalog`: the `service` content type becomes localized (en default + ar); seeding covers both locales; join key remains non-localized.
- `backend-api`: `/api/services` and the form-schema endpoints become locale-aware; form schemas get the translation overlay with graceful fallback.
- `frontend-app`: language switcher, RTL document direction, localized catalog/forms/chrome rendering.

## Impact

- `cms/`: i18n locale config, `service` schema update, new `form-translation` content type, bootstrap seeding for both locales (new fixture files).
- `backend/`: `StrapiClient` (locale param + form-translation fetch), form overlay logic, `ProcessController`/`TaskController`/`ServiceCatalogController` gain a `locale` request param.
- `frontend/`: language service + switcher in the app shell, `dir`/`lang` sync, chrome dictionary, locale param on API calls, CSS audit for logical properties.
- No new services, ports, or deployment requirements; `docker compose down -v` re-seeds both locales.
- Existing English behavior is unchanged when no locale (or `en`) is requested — purely additive.
