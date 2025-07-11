# Integration Testing Guide

This guide explains how to run the comprehensive integration tests that validate the Kotlin Notion Client's create/read/update/archive functionality.

## 🧪 Test Types

### 1. **Self-Contained Integration Tests** (`SelfContainedIntegrationTest.kt`)
These tests create their own test data, validate functionality, and clean up afterwards:

- **Database + Page Workflow**: Creates database → creates page → retrieves both → updates page → archives both
- **Standalone Page Test**: Creates standalone page → retrieves → archives  
- **Comprehensive Properties**: Tests database with all 10+ property types

### 2. **Simple API Tests** (`SimpleApiTest.kt`)
Basic tests that require pre-existing Notion objects (your existing setup).

## 🚀 Quick Start

### Prerequisites
1. **Notion Integration**: Create at https://www.notion.so/my-integrations
2. **Parent Page**: A page in your workspace where test databases can be created
3. **Permissions**: Your integration needs access to read/write pages and databases

### Setup Environment Variables
```bash
# Your integration API token
export NOTION_API_TOKEN="secret_ABC123..."

# ID of a page where test databases will be created  
export NOTION_PARENT_PAGE_ID="12345678-1234-1234-1234-123456789abc"
```

### Run Tests
```bash
# Option 1: Use the test runner script
./run-integration-tests.sh

# Option 2: Run directly with Gradle
./gradlew test --tests "*SelfContainedIntegrationTest*"

# Option 3: Run all integration tests
./gradlew test --tests "*SimpleApiTest*" --tests "*SelfContainedIntegrationTest*"
```

## 📋 What the Tests Validate

### ✅ Core CRUD Operations
- **Create**: Databases with multiple property types, pages in databases, standalone pages
- **Read**: Retrieve created objects and verify all properties
- **Update**: Modify page properties and verify changes
- **Archive**: Archive objects and verify they're marked as archived

### ✅ Real-World Scenarios
- **Database Schema**: Creates databases with 7-10 different property types
- **Page Properties**: Sets various property values (title, text, numbers, checkboxes, emails, etc.)
- **Parent Relationships**: Tests both database children and page children
- **Error Handling**: Validates proper API error responses

### ✅ Data Integrity
- **Type Safety**: Ensures all models serialize/deserialize correctly
- **ID Consistency**: Verifies created objects have consistent IDs
- **Property Preservation**: Confirms property values are preserved across operations
- **Cleanup Verification**: Ensures archived objects are actually archived

## 🧹 Test Data Cleanup

**Important**: These tests **archive** test data rather than permanently deleting it (Notion doesn't support true deletion). Archived objects:

- Are no longer visible in the Notion UI
- Can still be retrieved via API calls
- Don't count toward your Notion limits
- Can be restored if needed

The tests verify cleanup by confirming objects are marked as `archived: true`.

## 🔧 Test Architecture

### Naming Conventions
Our models use clear naming to avoid conflicts:

- **`DatabaseProperty`**: Property definitions/schemas (in databases)
- **`PagePropertyValue`**: Property values for requests (when creating/updating pages)  
- **`base.RichText`**: Rich text content structure

### Request Flow Example
```kotlin
// 1. Create database with schema
val database = client.databases.create(CreateDatabaseRequest(
    properties = mapOf(
        "Name" to CreateDatabaseProperty.Title(),        // Schema definition
        "Priority" to CreateDatabaseProperty.Select()    // Schema definition
    )
))

// 2. Create page with values
val page = client.pages.create(CreatePageRequest(
    parent = Parent(databaseId = database.id),
    properties = mapOf(
        "Name" to PagePropertyValue.TitleValue(...),     // Actual value
        "Priority" to PagePropertyValue.SelectValue(...) // Actual value
    )
))

// 3. Cleanup
client.pages.archive(page.id)
client.databases.archive(database.id)
```

## 🐛 Troubleshooting

### Common Issues

**❌ "Invalid API token"**
- Verify your `NOTION_API_TOKEN` is correct
- Check the integration hasn't been revoked

**❌ "Page not found"**  
- Verify your `NOTION_PARENT_PAGE_ID` exists
- Ensure your integration has access to the page

**❌ "Insufficient permissions"**
- Go to the parent page in Notion
- Click "..." → "Add connections" → Select your integration

**❌ "Rate limit exceeded"**
- The tests include small delays between operations
- If still hitting limits, increase delays in the test code

### Debug Mode
Run with verbose output to see detailed request/response logs:
```bash
NOTION_LOG_LEVEL=INFO ./run-integration-tests.sh
```

## 📊 Expected Output

Successful test run should show:
```
🧪 Running self-contained integration tests...

🗄️ Creating test database...
✅ Database created successfully: abc123...
📄 Creating test page in database...  
✅ Page created successfully: def456...
🔍 Retrieving database to verify structure...
🔍 Retrieving page to verify properties...
✏️ Updating page properties...
✅ Page updated successfully
🧹 Cleaning up - archiving page...
✅ Page archived successfully
🧹 Cleaning up - archiving database...
✅ Database archived successfully
🔍 Verifying cleanup...
✅ Cleanup verified - both objects are archived
🎉 Integration test completed successfully!
```

## 🎯 Next Steps

Once integration tests pass, you have a fully functional Notion API client that can:

1. **Create complex databases** with multiple property types
2. **Create and update pages** with rich property values
3. **Handle parent relationships** (database children, page children)
4. **Properly clean up** test data through archiving
5. **Serialize/deserialize** all data types correctly

This validates your client is ready for production use! 🚀