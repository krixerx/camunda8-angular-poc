## MODIFIED Requirements

### Requirement: Service catalog endpoint merges Camunda and Strapi
The backend SHALL expose `GET /api/services` returning one `ServiceCatalogItem` per latest deployed process definition, merging engine data (id, name, version, key) with the matching published Strapi `service` entry (title, summary, instructions, whatYouNeed, expectedDuration) joined on `processDefinitionId`. Strapi SHALL be read via a dedicated client with an env-overridable base URL (localhost in dev, service DNS in Docker) and a short request timeout. The endpoint SHALL accept an optional `locale` query parameter (`en` default, `ar` supported; unknown values treated as `en`): for a non-default locale the backend SHALL fetch that locale's entries from Strapi and SHALL fall back per service to the English entry when no localization exists. Content fields SHALL be null when no Strapi entry matches in any locale. The endpoint SHALL require a valid bearer token (any realm role), consistent with other read endpoints. `ServiceCatalogItem` SHALL follow the project DTO convention: a separate record in the `dto` package with a static `from()` mapper.

#### Scenario: Merged catalog item
- **WHEN** the frontend requests `GET /api/services` while `vehicle-registration` is deployed and has a published Strapi entry
- **THEN** the response item for `vehicle-registration` carries both the engine fields and the editorial content fields

#### Scenario: Arabic catalog content
- **WHEN** the frontend requests `GET /api/services?locale=ar` and both services have published Arabic localizations
- **THEN** the content fields of both items carry the Arabic text

#### Scenario: Missing Arabic localization falls back to English
- **WHEN** the frontend requests `GET /api/services?locale=ar` and a service has an English entry but no Arabic localization
- **THEN** that item's content fields carry the English text (not null, no error)

#### Scenario: Definition without CMS content
- **WHEN** a process definition is deployed with no matching published Strapi entry
- **THEN** the catalog item is still returned with engine fields populated and content fields null

#### Scenario: Strapi unavailable degrades gracefully
- **WHEN** the frontend requests `GET /api/services` while Strapi is down or times out
- **THEN** the backend returns HTTP 200 with engine-only items (all content fields null) and logs a warning — it does not return an error

#### Scenario: Orphaned CMS entry not shown
- **WHEN** a published Strapi entry references a `processDefinitionId` that has no deployed process definition
- **THEN** no catalog item is produced for it

## ADDED Requirements

### Requirement: Form schemas are served with a locale-aware translation overlay
The form-schema endpoints (`GET /api/process-definitions/{key}/form` and the form schema inside `GET /api/tasks/{key}`) SHALL accept an optional `locale` query parameter (`en` default, `ar` supported; unknown values treated as `en`). For a non-default locale the backend SHALL fetch the published `form-translation` entry matching the schema's form id and overlay its strings onto the schema before returning it: field `label`, `description`, `placeholder`, select/radio option labels (matched by option value), and text-view content. The overlay SHALL NOT modify field `key`, `type`, validation rules, conditionals, or any FEEL expression. Strings without a translation SHALL keep their authored English value. When Strapi is unavailable or no translation entry exists, the schema SHALL be returned unmodified — never an error. For the default locale the backend SHALL NOT contact Strapi and SHALL return the schema byte-for-byte as today.

#### Scenario: Arabic start form
- **WHEN** the frontend requests the `vehicle-registration` start form with `locale=ar`
- **THEN** the returned schema carries Arabic labels, descriptions, and select option labels while field keys, types, and validation are identical to the deployed schema

#### Scenario: Untranslated string keeps authored text
- **WHEN** a form field has no entry in the Arabic `strings` map
- **THEN** that field's authored English label is returned unchanged alongside the translated fields

#### Scenario: Strapi down during Arabic form request
- **WHEN** the frontend requests a form schema with `locale=ar` while Strapi is unreachable
- **THEN** the backend returns HTTP 200 with the unmodified authored schema and logs a warning

#### Scenario: English requests bypass translation entirely
- **WHEN** the frontend requests a form schema without a `locale` parameter (or with `locale=en`)
- **THEN** the backend does not call Strapi for form translations and the schema matches the deployed one exactly
