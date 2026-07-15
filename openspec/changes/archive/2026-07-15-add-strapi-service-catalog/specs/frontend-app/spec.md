## MODIFIED Requirements

### Requirement: Services page starts processes
A Services page SHALL list the service catalog from `GET /api/services`, rendering the editorial title and summary from CMS content on each service card; when a catalog item has no CMS content, the card SHALL fall back to the engine data (definition name and id). Starting a service SHALL render its start form (if any) with form-js and submit the values to `POST /api/process-definitions/{key}/start`; when the catalog item carries `instructions`, the start page SHALL display them above the start form.

#### Scenario: Start a process from the catalog
- **WHEN** the user opens the Services page, chooses "Vehicle registration", fills the start form, and submits
- **THEN** a new process instance is created and the user is navigated to a confirmation (or the created task)

#### Scenario: Card shows editorial content
- **WHEN** the Services page loads and `vehicle-registration` has published CMS content
- **THEN** its card shows the CMS title and summary instead of the raw engine name

#### Scenario: Card falls back to engine data
- **WHEN** the Services page loads and a deployed definition has no CMS content (or Strapi is down)
- **THEN** its card still renders with the engine definition name and remains startable

#### Scenario: Instructions shown before the start form
- **WHEN** the user opens the start page for a service whose catalog item carries `instructions`
- **THEN** the instructions are rendered above the start form
