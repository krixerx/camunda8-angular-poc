# Form: business-registration-start

Start form of `business-registration`. Linked to the process start event; rendered by the frontend Services page before starting an instance.

## Fields

| Key | Label | Type | Required | Validation / options |
|---|---|---|---|---|
| `companyName` | Company name | textfield | yes | min length 3; suffix "OÜ" not enforced |
| `shareCapital` | Share capital (EUR) | number | yes | min 1 |
| `founderAge` | Founder age | number | yes | min 1, max 120 |

## Output

All field values become process variables verbatim (`companyName`, `shareCapital`, `founderAge`).
