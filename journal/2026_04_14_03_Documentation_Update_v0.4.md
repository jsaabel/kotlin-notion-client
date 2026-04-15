# Documentation Update — v0.4.0

**Date**: 2026-04-14

## Summary

Completed a full documentation pass to bring all docs up to date with the v0.4.0 feature branch.
All items from `docs/doc-update-plan.md` were addressed.

## Changes Made

- **README.md**: Version bump to 0.4.0, API version to 2026-03-11, new API Coverage rows (Markdown, Custom Emojis, Views), Feature Highlights updated, notebook note updated.
- **docs/pages.md**: Archive → Trash rename, new Markdown Content section, Verification Property section, native icon example, best practice #6 updated.
- **docs/databases.md**: Status property, property/option descriptions, native icon, API version.
- **docs/data-sources.md**: Relative date filters, `containsMe()` / `doesNotContainMe()` people filter, API version.
- **docs/comments.md**: Markdown comment creation section, validation note updated.
- **docs/blocks.md**: Block count 30+ → 33+, `delete` description fix, `heading_4`/`tab`/`meeting_notes` in supported types, examples 14 & 15, native icon in callout.
- **docs/markdown-api.md**: New file — full Markdown Content API reference.
- **docs/views-api.md**: New file — user-facing Views API guide.
- **docs/testing.md**: Test count 481 → 600+, `NOTION_CLEANUP_AFTER_TEST` default corrected, integration test file names updated, `NOTION_TEST_WIKI_PAGE_ID` added.
- **docs/custom-emojis.md**: New file — Custom Emoji listing API.
- **notebooks/**: All 7 notebooks bumped to `0.4.0`. Binary incompatibility warning removed from `01-getting-started.ipynb`. Relative date filter examples added to `02-reading-databases.ipynb`. `README.md` updated.
- **notebooks/08-whats-new-in-v0.4.ipynb**: New notebook — guided tour of all v0.4.0 changes.

## Open Questions / Follow-up Needed

### Markdown API — explicit line break behaviour

The markdown API documentation notes that explicit `\n` characters within a markdown string are
interpreted as paragraph separators. The exact behaviour around line breaks in the request body
needs further validation, specifically:

- Does a single `\n` create a line break within a paragraph, or always start a new paragraph?
- Does `\n\n` reliably create a paragraph break in all block contexts?
- Are there differences between `replaceContent`, `updateContent`, and the `markdown()` DSL in
  `CreatePageRequestBuilder`?

**Action**: Test with live API before finalising the Markdown API docs. The current docs note this
uncertainty in a "Known behaviours / gotchas" callout.
