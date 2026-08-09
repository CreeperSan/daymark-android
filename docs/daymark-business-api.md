# Daymark business API boundary

The Android client uses an independently configurable Daymark service host. Its default is
`https://daymark.creepersan.com/`; tests can inject a replacement through `DaymarkServiceConfig`.

## Confirmed server operations

| Client operation | Endpoint | Notes |
| --- | --- | --- |
| Create anniversary | `POST /api/anniversary/add` | Requires name, date, calendar type, and loop type. |
| Edit anniversary | `POST /api/anniversary/edit` | Nullable fields are omitted when unchanged. |
| Delete anniversary | `POST /api/anniversary/delete` | Requires anniversary ID. |
| Get anniversary | `POST /api/anniversary/get` | Requires anniversary ID. |
| List anniversaries | `POST /api/anniversary/list` | Supports name, calendar type, page, and page size query parameters. |
| Sort anniversaries | `POST /api/anniversary/sort` | Accepts ordered ID list. |
| Refresh anniversaries | `POST /api/anniversary/refresh` | Refreshes next-occurrence timestamps. |

All confirmed requests use the account token and device headers supplied by the account module.
`DaymarkRepository` maps transport, decoding, parameter, authentication, and server failures to
`DaymarkError`; callers must not treat a failure as an empty successful response.

## Placeholder replacement points

The server does not currently expose confirmed APIs for Mark media, timeline events, categories,
reminders, or version migration. Their screens use local page state and close with
`CSPageOutcome.Unavailable` rather than reporting remote persistence. When an API is added:

1. Add a typed request/response contract to `module/daymark`.
2. Add a repository method that maps the server response to a typed result.
3. Replace the placeholder action while preserving `CSBusinessPageEnter` and
   `CSBusinessPageExit` contracts.
