# API Model Assumptions (2025-09-03)

This document tracks assumptions we've made about the Notion API models based on official sample responses. These should be validated against the full API documentation and real-world usage.

## Database Model

### Fields Made Optional (Based on Official Samples)

**Sample Used**: `src/test/resources/api/databases_new/get_retrieve_a_database.json`

| Field | Assumption | Reasoning | Needs Verification |
|-------|------------|-----------|-------------------|
| `description` | Optional (default: empty list) | Not present in official sample response | ⚠️ YES - May only be missing in minimal samples |
| `url` | Optional (nullable) | Not present in official sample response | ⚠️ YES - FAQ mentions database URLs exist, but sample doesn't include it |
| `created_by` | Already optional | Standard pattern, may be null for old objects | ✓ Safe assumption |
| `last_edited_by` | Already optional | Standard pattern, may be null for old objects | ✓ Safe assumption |

### Questions to Answer

1. **Database URL**: The FAQ (line 21-27 in `reference/notion-api/upgrading_to_2025_09_03/faq.md`) states that databases have URLs in the app. Why is it missing from the sample response?
   - Is it deprecated in the API but still shown in the UI?
   - Is it only returned in certain contexts?
   - Is the sample incomplete?

2. **Database Description**: Can databases have descriptions in the 2025-09-03 API?
   - If yes, under what conditions is it included/excluded from responses?
   - Is it now on the data source instead?

### Action Items

- [ ] Test with real API to see if `url` is actually returned
- [ ] Check if description moved to data source model
- [ ] Update model based on findings
- [ ] Consider making `url` non-nullable with a TODO comment if real API always returns it

## Data Source Model

_To be documented as we work with it_

## Page Model

_To be documented as we work with it_

---

**Note**: These assumptions are based on limited official samples. Always prefer real API behavior and comprehensive documentation when available.
