# Form: review-application

Linked Camunda Form of user task `review-application` in `business-registration`. Only reached when the DMN decision did not auto-approve.

## Fields

| Key | Label | Type | Required | Notes |
|---|---|---|---|---|
| `companyName` | Company name | textfield | — | read-only (prefilled from variables) |
| `shareCapital` | Share capital (EUR) | number | — | read-only |
| `founderAge` | Founder age | number | — | read-only |
| `approved` | Approve this application | checkbox | no | unchecked = reject; defaults to false |

## Output

`approved` (boolean) drives the second exclusive gateway (`= approved` → Registered, else Rejected).
