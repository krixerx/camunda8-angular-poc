# Form: vehicle-registration-start

Start form of `vehicle-registration`. Linked to the process start event; rendered by the frontend Services page before starting an instance.

## Fields

| Key | Label | Type | Required | Validation / options |
|---|---|---|---|---|
| `ownerName` | Owner name | textfield | yes | min length 2 |
| `vin` | VIN | textfield | yes | pattern `[A-HJ-NPR-Z0-9]{17}` (17 chars, no I/O/Q) |
| `category` | Vehicle category | select | yes | `car` (Car), `motorcycle` (Motorcycle), `truck` (Truck) |

## Output

All field values become process variables verbatim (`ownerName`, `vin`, `category`).
