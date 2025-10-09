# Webhooks

Webhooks let your integration receive **real-time updates** from Notion. Whenever a page or database changes, Notion sends a secure HTTP `POST` request to your webhook endpoint. This allows your application to react immediately — syncing data, triggering automation, or updating your UI.

> Instead of repeatedly polling the Notion API to check for changes, Notion will **notify** you when something important happens.

---

## How Webhooks Work: A Simple Example

1. Your integration is subscribed to `page.content_updated` events.
2. A user edits the title of a page in Notion.
3. Within a minute, Notion sends a webhook request to your configured endpoint.
4. The payload includes metadata like the page ID, event type, and timestamp.
5. Your server verifies the event and uses the page ID to call the Notion API.
6. Your app updates its internal data or triggers other actions.

---

## Getting Started with Webhooks

### Step 1: Creating a Webhook Subscription

To receive webhook events, create a subscription in your integration settings:

- Visit your [integration settings](https://www.notion.so/my-integrations).
- Create or select an existing integration.
- Go to the **Webhooks** tab and click **+ Create a subscription**.
- Enter your public **Webhook URL** (must use HTTPS and be publicly accessible).
- Select the event types to subscribe to.
- Click **Create subscription**.

> At this point, the webhook is created but **not yet verified**.

---

### Step 2: Verifying the Subscription

Notion will send a one-time `POST` request with a `verification_token` to your webhook URL.

#### Example Payload

```json
{
  "verification_token": "secret_tMrlL1qK5vuQAh1b6cZGhFChZTSYJlce98V0pYn7yBl"
}

To verify:
	1.	Inspect the request at your endpoint and extract the verification_token.
	2.	(Optional) Store the token securely for later use.
	3.	Return to the Webhooks tab in your integration UI.
	4.	Click ⚠️ Verify, paste the token, and submit.

If you didn’t receive a token, click Resend token in the verification modal.

Once submitted, the subscription becomes active.

🔐 You can only change the webhook URL before verification.
After that, you’ll need to delete and recreate the subscription.
You can change subscribed events at any time.

⸻

Step 3: Validating Event Payloads (Recommended)

To ensure security, every webhook request includes an X-Notion-Signature header, which is an HMAC-SHA256 hash of the payload, signed with your verification_token.

Example Header

{
  "X-Notion-Signature": "sha256=461e8cbcba8a75c3edd866f0e71280f5a85cbf21eff040ebd10fe266df38a735"
}

JavaScript Example

import { createHmac, timingSafeEqual } from "crypto"

const verificationToken = "your_token_here"
const body = {...} // your parsed request body

const calculatedSignature = `sha256=${createHmac("sha256", verificationToken)
  .update(JSON.stringify(body))
  .digest("hex")}`

const isTrustedPayload = timingSafeEqual(
  Buffer.from(calculatedSignature),
  Buffer.from(headers["X-Notion-Signature"]),
)

if (!isTrustedPayload) {
  // Ignore the event
  return
}

If you’re using a no-code/low-code platform like Zapier, Make, or Pipedream, you may not have access to custom code for validation. That’s okay — validation is recommended, not required.

⸻

Testing Your Webhook Subscription

Test 1: Change a Page Title
	1.	Add the integration to a page.
	2.	Edit the page title.
	3.	Wait 1–2 minutes (aggregated event).
	4.	You should receive a page.content_updated event.
	5.	Use the entity.id to call retrieve a page and confirm the change.

Test 2: Add a Comment
	1.	Add a comment to a page your integration has access to.
	2.	You should receive a comment.created event.

🛠 Requires comment read capability.

Test 3: Modify a Database Schema
	1.	Open a database your integration is connected to.
	2.	Add, rename, or delete a property.
	3.	You should receive a database.schema_updated event.

⸻

Troubleshooting Tips

🔒 1. Check Access Permissions

Make sure your integration has access to the object that triggered the event. If the object is private, you may not receive an event.

✅ 2. Confirm Capabilities

Some event types require specific capabilities.

Example: comment.created requires the comment read capability.

Check your integration’s Capabilities tab.

⏳ 3. Understand Aggregated Event Timing

Some events (like page.content_updated) are batched to reduce noise.

For instant feedback, test with comment.created or page.locked.

☑️ Confirm Your Subscription Status

Verify that the webhook subscription is active under your integration settings. If the status is paused, pending verification, or deleted — events won’t be delivered.

