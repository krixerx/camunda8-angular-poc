## MODIFIED Requirements

### Requirement: Strapi hosts the service content type
A Strapi v5 application (`cms/`, SQLite storage, version pinned) SHALL define a `service` collection type with draft & publish enabled and the fields: `title` (string, required), `summary` (text), `instructions` (rich text), `whatYouNeed` (rich text), `expectedDuration` (string), and `processDefinitionId` (string, required, unique) — the join key matching the BPMN process id of a deployed Camunda process definition. The collection SHALL use Strapi i18n with locales `en` (default) and `ar`: the content fields SHALL be localized per locale, while `processDefinitionId` SHALL be non-localized so all locale versions of a document share the join key. The content-type schema and locale configuration SHALL be committed to the repository.

#### Scenario: Content model present after boot
- **WHEN** Strapi starts and an editor opens the admin panel content manager
- **THEN** the `service` collection type exists with the specified fields, draft & publish enabled, and an `en`/`ar` locale switcher

#### Scenario: Editor updates catalog copy without a deployment
- **WHEN** an editor changes the `summary` of a published `service` entry in the Strapi admin panel and saves + publishes
- **THEN** the updated text is served by the Strapi API without rebuilding or restarting any other component

#### Scenario: Arabic localization shares the join key
- **WHEN** an editor opens the Arabic locale version of a `service` document
- **THEN** `processDefinitionId` is the same value as in the English version and is not independently editable per locale

### Requirement: Seed content on first boot
The Strapi bootstrap hook SHALL create and publish `service` entries for `vehicle-registration` and `business-registration` in both `en` and `ar` locales from committed fixtures when no `service` entries exist, and SHALL add missing `ar` localizations to existing English documents (upgrade path for pre-i18n volumes). Seeding SHALL be idempotent: it is skipped when the corresponding content already exists, so editor changes survive restarts.

#### Scenario: Fresh stack starts populated in both locales
- **WHEN** the stack starts on a fresh volume (`docker compose up` after `down -v`)
- **THEN** the Strapi API serves published entries for both services in `en` and in `ar` without any manual step

#### Scenario: Existing English volume gains Arabic content
- **WHEN** a Strapi volume seeded before i18n starts with the new image
- **THEN** the existing English entries are untouched and Arabic localizations are seeded for them

#### Scenario: Editor changes survive restart
- **WHEN** an editor edits a seeded entry and the Strapi container is restarted (volume retained)
- **THEN** the edited content is served and the seed fixture does not overwrite it
