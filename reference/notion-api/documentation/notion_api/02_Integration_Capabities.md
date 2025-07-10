# Integration Capabilities

All integrations have associated **capabilities** which enforce what an integration can do and see in a Notion workspace. These capabilities, when combined, determine which API endpoints an integration can call and what content and user-related information it can access.  
To set your integration's capabilities, see the [Authorization guide](https://developers.notion.com/docs/authorization) or navigate to [https://www.notion.so/my-integrations](https://www.notion.so/my-integrations).

> 📘 **Note**  
> If an integration is added to a page, then the integration can access the page’s children.
>
> When an integration receives access to a Notion page or database, it can read and write to both that resource and its children.

---

## Content Capabilities

Content capabilities affect how an integration can interact with **database objects**, **page objects**, and **block objects** via the API. They also affect what information is exposed to an integration in API responses.  
Refer to the API references to verify which capabilities are required for a specific endpoint.

- **Read content**:  
  Allows an integration to read existing content in a Notion workspace.  
  _Example_: Can call `Retrieve a database`, but **not** `Update database`.

- **Update content**:  
  Allows an integration to update existing content.  
  _Example_: Can call `Update page`, but **not** create new pages.

- **Insert content**:  
  Allows an integration to create new content in a workspace, but **not** read full objects.  
  _Example_: Can call `Create a page`, but **not** update existing pages.

> Integrations may have any combination of these content capabilities.

---

## Comment Capabilities

These capabilities define how an integration can interact with **comments** on a page or block.

- **Read comments**:  
  Allows reading comments from a Notion page or block.

- **Insert comments**:  
  Allows inserting comments in a page or an existing discussion.

---

## User Capabilities

User capabilities determine what user-related data is available to the integration:

- **No user information**:  
  User objects will contain no data — no name, profile image, or email address.

- **User information without email addresses**:  
  User objects will include name and profile image, but **not** the email address.

- **User information with email addresses**:  
  User objects will include name, profile image, and email address.

---

## Capability Behaviors and Best Practices

- An integration's capabilities **do not override** a user's permissions.  
  If a user loses edit access to a page where they added an integration, the integration will now also only have read access — regardless of its configured capabilities.

- For **public integrations**, users will need to **re-authenticate** if capabilities have changed since their last authorization.

- **Follow the principle of least privilege**:  
  Request only the minimum capabilities your integration needs to function.  
  This increases the likelihood of workspace admins approving your integration.

### Examples

- **Creating content in Notion** (e.g., adding pages or blocks):  
  _Requires only_ `Insert content` capabilities.

- **Reading data from Notion to export elsewhere**:  
  _Requires only_ `Read content` capabilities.

- **Updating existing pages or blocks**:  
  _Requires only_ `Update content` capabilities.