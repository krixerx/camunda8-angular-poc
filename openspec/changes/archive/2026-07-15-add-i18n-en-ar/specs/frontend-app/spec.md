## ADDED Requirements

### Requirement: UI language switches between English and Arabic
The app shell SHALL offer an EN/AR language switcher. The selected language SHALL persist in `localStorage` (key `c8poc.lang`) and survive reloads. On language change (and on startup) the app SHALL set `document.documentElement.lang` to the active locale and `dir` to `rtl` for Arabic / `ltr` for English, and re-render visible content in the selected language without a page reload. App chrome strings (navigation, buttons, empty states, generic errors) SHALL come from a developer-owned in-repo dictionary covering both languages; layout SHALL use direction-safe CSS (logical properties) so the RTL flip does not break pages.

#### Scenario: Switch to Arabic
- **WHEN** the user clicks the AR option in the switcher on the Services page
- **THEN** the document direction becomes `rtl`, chrome strings render in Arabic, and the catalog reloads with Arabic content — without a full page reload

#### Scenario: Language survives reload
- **WHEN** the user selects Arabic and reloads the browser
- **THEN** the app starts in Arabic with `dir="rtl"` without the user touching the switcher again

#### Scenario: Layout integrity in RTL
- **WHEN** the Services, start-form, and task pages render in Arabic
- **THEN** cards, forms, and navigation lay out mirrored (start-aligned for RTL) with no overlapping or clipped elements

### Requirement: Localized content is requested per active language
While Arabic is active, API calls for catalog content and form schemas SHALL carry `locale=ar` (`GET /api/services`, the start-form fetch, and task detail); English SHALL use today's parameterless requests. Editorial content and form labels SHALL render in the active language, falling back to English text delivered by the backend when a translation is missing.

#### Scenario: Arabic catalog and start form
- **WHEN** Arabic is active and the user opens the Services page and then a start page
- **THEN** card titles/summaries and the start form's labels and instructions render in Arabic

#### Scenario: Arabic task form
- **WHEN** Arabic is active and a civil servant opens a review task
- **THEN** the task form labels render in Arabic and completing the task works exactly as in English
