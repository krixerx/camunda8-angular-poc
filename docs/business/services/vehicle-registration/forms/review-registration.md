# Form: review-registration

Linked Camunda Form of user task `review-registration` in `vehicle-registration`.

## Fields

| Key | Label | Type | Required | Notes |
|---|---|---|---|---|
| `ownerName` | Owner name | textfield | — | read-only (prefilled from variables) |
| `vin` | VIN | textfield | — | read-only |
| `category` | Vehicle category | textfield | — | read-only |
| `price` | Registration fee (EUR) | number | — | read-only, set by the price worker |
| `approved` | Approve this registration | checkbox | no | unchecked = reject; defaults to false |

## Output

`approved` (boolean) drives the exclusive gateway (`= approved` → Registered, else Rejected). Read-only fields are display-only and MUST NOT overwrite process variables on completion.
