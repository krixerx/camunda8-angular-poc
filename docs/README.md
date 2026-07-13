# Docs index

| Doc | Answers |
|---|---|
| [architecture.md](architecture.md) | What runs where, ports, request flow, security posture, conventions |
| [business/services/vehicle-registration/](business/services/vehicle-registration/README.md) | Vehicle registration process: flow, variables, forms, worker behavior |
| [business/services/business-registration/](business/services/business-registration/README.md) | Business registration process: flow, variables, forms, DMN decision |
| `../openspec/changes/bootstrap-poc/` | Why/what/how of the bootstrap change + implementation task checklist |

Conventions: one folder per service under `business/services/`, with `README.md` (flow + variables), `forms/<form-id>.md` (field tables), `decisions/<decision-id>.md` (DMN tables). Analyst owns this markdown; BPMN/DMN/form files are generated/authored to match it, never the other way around. Update docs in the same commit as the artifacts they describe.
