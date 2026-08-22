# Status Option Groups

> Source: https://developers.notion.com/reference/property-object (status section), 2026-06-22 changelog
> Fetched: 2026-08-22 (API version `2026-03-11`)

Status properties group their options into three fixed buckets, mirroring
the grouping shown in Notion's UI. As of the 2026-06-22 changelog, the API
lets a create/update request assign that grouping explicitly via an optional
`group` field on each option — previously this was UI-only.

## Option object

```json
{
  "id": "034ece9a-384d-4d1f-97f7-7f685b29ae9b",
  "name": "Not started",
  "color": "default",
  "group": "To-do"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | string | Assigned by Notion; omit when creating a new option |
| `name` | string | Must be unique case-insensitively within the property; commas not allowed |
| `color` | string | One of `blue`, `brown`, `default`, `gray`, `green`, `orange`, `pink`, `purple`, `red`, `yellow` |
| `group` | string, optional | One of `"To-do"`, `"In progress"`, `"Complete"` |

## Behavior

- **Creating a status property with no options specified**: Notion
  auto-generates the standard three — "Not started" (group "To-do"),
  "In progress" (group "In progress"), "Done" (group "Complete").
- **Creating/updating with `group` set on an option**: the option is placed
  in that group.
- **Updating an existing option with `group` omitted**: the option **keeps
  its current group** — omitting the field is not the same as clearing it.
- **A brand-new option added on update with `group` omitted**: defaults to
  `"To-do"`.

## This client

`StatusConfiguration.options` models this (see `FOLLOWUPS.md` items 23/24 for
the open verification/ratification status — implemented at high confidence
from these live docs but not yet exercised against a live API run).
