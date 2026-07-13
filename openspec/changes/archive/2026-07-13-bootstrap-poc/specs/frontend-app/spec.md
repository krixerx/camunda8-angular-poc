# frontend-app

## ADDED Requirements

### Requirement: Angular SPA talks only to the backend
The `frontend/` module SHALL be an Angular 22 application (standalone components, strict TypeScript) that calls only the backend `/api/**` endpoints — never the Camunda API directly. In development, `ng serve` SHALL proxy `/api` to `http://localhost:8085`.

#### Scenario: Dev proxy works
- **WHEN** the developer runs `npm start` in `frontend/` with the backend running
- **THEN** the app at `http://localhost:4200` loads data through the proxied `/api` endpoints

### Requirement: Services page starts processes
A Services page SHALL list the deployed process definitions from `GET /api/process-definitions`. Starting a service SHALL render its start form (if any) with form-js and submit the values to `POST /api/process-definitions/{key}/start`.

#### Scenario: Start a process from the catalog
- **WHEN** the user opens the Services page, chooses "Vehicle registration", fills the start form, and submits
- **THEN** a new process instance is created and the user is navigated to a confirmation (or the created task)

### Requirement: Tasks page lists and opens user tasks
A Tasks page SHALL list open user tasks from `GET /api/tasks`. Opening a task SHALL show a Task detail view that renders the task's Camunda Form schema with `@bpmn-io/form-js-viewer`, pre-filled from current variables, and completes the task via `POST /api/tasks/{key}/complete`.

#### Scenario: Complete a review task
- **WHEN** the user opens a "Review registration" task, edits the form, and submits
- **THEN** the task completes, the user returns to the Tasks page, and the task is gone from the list

### Requirement: form-js wrapper component
Camunda Form rendering SHALL be encapsulated in a single reusable Angular component that instantiates the form-js viewer on an element ref, imports the required form-js CSS, emits submit events with `{ data, errors }`, blocks submission when validation errors exist, and destroys the viewer on component teardown.

#### Scenario: Client-side validation
- **WHEN** the user submits a form with a required field empty
- **THEN** form-js shows the validation error and no request is sent to the backend

### Requirement: Processes page shows instances
A Processes page SHALL list process instances from `GET /api/process-instances` with their state (active/completed) and start date.

#### Scenario: Completed instance visible
- **WHEN** a process instance finishes and the user opens the Processes page
- **THEN** the instance is listed with state "completed"
