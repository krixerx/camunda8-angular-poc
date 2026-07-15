# cms-form-translations Specification

## Purpose
TBD - created by syncing change add-i18n-en-ar. Update Purpose after archive.
## Requirements
### Requirement: Strapi hosts form translations
Strapi SHALL define a `form-translation` collection type with draft & publish enabled and the fields: `formId` (string, required, unique, non-localized) — the kebab-case Camunda form id from the deployed `.form` schema — and `strings` (JSON, localized) holding presentational strings keyed by form field key: `label`, `description`, `placeholder`, select/radio option labels keyed by option value, and text-view content. The collection SHALL use Strapi i18n with locales `en` (default) and `ar`; only non-default locales carry translations (authored English in the `.form` files is the fallback, so each string has exactly one source per language). The bootstrap hook SHALL grant the public role `find` and `findOne` on `form-translation`; writes and the admin panel SHALL remain protected by Strapi's own authentication.

#### Scenario: Backend reads Arabic form translations
- **WHEN** a client requests the published `form-translation` entry for `vehicle-registration-start` with locale `ar` without credentials
- **THEN** Strapi returns HTTP 200 with the Arabic `strings` map for that form

#### Scenario: Editor fixes a form label without a deployment
- **WHEN** an editor edits the Arabic label for a field inside a `form-translation` entry and publishes
- **THEN** the updated label is served by the Strapi API without redeploying the form or restarting any component

### Requirement: Form translations are seeded on first boot
The Strapi bootstrap hook SHALL create and publish `form-translation` entries with Arabic strings for all deployed forms (`vehicle-registration-start`, `review-registration`, `business-registration-start`, `review-application`) from a committed fixture when none exist. Seeding SHALL be idempotent: skipped when entries already exist, so editor changes survive restarts and re-runs.

#### Scenario: Fresh stack has Arabic form translations
- **WHEN** the stack starts on a fresh volume
- **THEN** the Strapi API serves published Arabic `form-translation` entries for all four forms without any manual step

#### Scenario: Seeding does not overwrite editor changes
- **WHEN** an editor has modified a `form-translation` entry and Strapi restarts
- **THEN** the edited translation is served and the seed fixture does not overwrite it
