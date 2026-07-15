## 1. Strapi i18n content model (`cms/`)

- [x] 1.1 Enable locales `en` (default) + `ar` (`cms/config/plugins.js` i18n config) and localize the `service` schema: content fields `localized: true`, `processDefinitionId` non-localized; commit schema JSON
- [x] 1.2 Add `form-translation` collection type: `formId` (string, required, unique, non-localized), `strings` (JSON, localized); draft & publish on
- [x] 1.3 Author fixtures: `seed-services.ar.json` (Arabic catalog copy for both services) and `seed-form-translations.json` (Arabic strings for `vehicle-registration-start`, `review-registration`, `business-registration-start`, `review-application`)
- [x] 1.4 Extend the bootstrap hook: grant public `find`/`findOne` on `form-translation`; seed `service` in `en`+`ar` and `form-translation` in `ar`; add missing `ar` localizations to existing English documents (pre-i18n volume upgrade); keep everything idempotent
- [x] 1.5 Verify against a running container: `GET :1337/api/services?locale=ar` serves Arabic entries; missing-locale behavior confirmed; `form-translation` public read works; document the actual Strapi v5 locale query shape for the backend

## 2. Backend locale support

- [x] 2.1 `StrapiClient`: locale param on the service fetch + `fetchFormTranslations(locale)`; per-service English fallback merge for partial `ar` coverage
- [x] 2.2 `ServiceCatalogController`: optional `locale` request param (`en` default, unknown → `en`) wired through the merge
- [x] 2.3 `FormTranslator`: overlay `label`/`description`/`placeholder`/option labels/text views onto a form schema; never touch `key`, `type`, `validate`, `conditional`, or FEEL expressions; untranslated strings keep authored text
- [x] 2.4 Wire overlay into `GET /api/process-definitions/{key}/form` and `GET /api/tasks/{key}` behind the `locale` param; `en` path bypasses Strapi entirely
- [x] 2.5 Backend tests: Arabic catalog merge + English fallback, overlay translates presentational strings only (schema structure assertion), untranslated-field fallback, Strapi-down returns unmodified schema, `en` bypass (`./mvnw test`)

## 3. Frontend language switcher + RTL

- [x] 3.1 `core/language.service.ts`: locale signal, `localStorage` key `c8poc.lang`, sets `document.documentElement.lang`/`dir` on init and change
- [x] 3.2 `core/i18n.ts`: chrome dictionary (nav, buttons, empty states, error fallbacks) for `en`/`ar`; app shell + pages consume it
- [x] 3.3 EN/AR switcher in the app shell header
- [x] 3.4 `ApiService`: append `locale` to services/start-form/task-detail calls when Arabic is active; pages re-fetch on language change
- [x] 3.5 CSS audit: replace physical direction properties with logical ones in `styles.css` and page styles; verify form-js renders correctly under `dir="rtl"`
- [x] 3.6 Frontend build + unit tests incl. switcher persistence and Arabic services-page rendering (`npx ng build`, `npx ng test`)

## 4. Docs

- [x] 4.1 README: language switcher + Arabic/RTL in the feature list, i18n note in the Strapi editing section
- [x] 4.2 `docs/architecture.md`: locale flow (param through the facade, overlay seam, fallback chain), i18n in the content ownership boundary
- [x] 4.3 CLAUDE.md: locale conventions, form-translation gotchas
- [x] 4.4 Business-spec READMEs: Arabic seeded copy tables for both services (flag as developer-written, pending native review)

## 5. End-to-end verification

- [x] 5.1 Clean slate `docker compose up --build`: switch to AR → catalog, start form, and task form render Arabic RTL; complete a full bart→homer flow in Arabic
- [x] 5.2 Fallback: remove one Arabic localization → that service falls back to English while others stay Arabic; stop Strapi → Arabic UI still functional with authored English forms and engine catalog data
- [x] 5.3 Editorial loop: edit an Arabic label in the Strapi admin, publish, reload → new text appears without redeploy
- [x] 5.4 English regression: default (no locale) behavior identical to pre-change — schema byte-for-byte, no extra Strapi calls
