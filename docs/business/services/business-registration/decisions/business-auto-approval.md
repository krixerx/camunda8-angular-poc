# Decision: business-auto-approval

DMN decision table called by the business rule task in `business-registration` (`bindingType="deployment"`, result variable `autoApproved`).

- **Decision id:** `business-auto-approval`
- **Hit policy:** FIRST
- **Inputs:** `shareCapital` (number), `founderAge` (number)
- **Output:** `autoApproved` (boolean)

## Decision table

| # | shareCapital | founderAge | autoApproved | Annotation |
|---|---|---|---|---|
| 1 | `>= 2500` | `>= 18` | `true` | Fully paid minimum capital + adult founder → no review needed |
| 2 | `-` | `-` | `false` | Everything else goes to manual review |

## Notes

- Thresholds mirror the cib7 POC's business-registration rule (EUR 2500 minimum share capital, adult founder).
- FEEL number comparisons; inputs must arrive as numbers (the start form uses number fields).
- Rule 2 is the catch-all; with hit policy FIRST the order matters — keep it last.
