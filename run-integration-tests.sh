#!/bin/bash

# Kotlin Notion Client - Integration Test Runner
# 
# This script helps you run the self-contained integration tests that create
# their own test data and clean up afterwards.

echo "🚀 Kotlin Notion Client - Integration Test Runner"
echo "=================================================="

# Check if environment variables are set
if [ -z "$NOTION_API_TOKEN" ]; then
    echo "❌ NOTION_API_TOKEN environment variable is not set"
    echo "   Get your token from: https://www.notion.so/my-integrations"
    echo "   Export it: export NOTION_API_TOKEN='secret_...'"
    exit 1
fi

if [ -z "$NOTION_PARENT_PAGE_ID" ]; then
    echo "❌ NOTION_PARENT_PAGE_ID environment variable is not set"
    echo "   This should be a page ID where test databases can be created"
    echo "   Export it: export NOTION_PARENT_PAGE_ID='12345678-1234-1234-1234-123456789abc'"
    exit 1
fi

echo "✅ Environment variables are set"
echo "   NOTION_API_TOKEN: ${NOTION_API_TOKEN:0:10}..."
echo "   NOTION_PARENT_PAGE_ID: $NOTION_PARENT_PAGE_ID"
echo ""

# Run the tests
echo "🧪 Running self-contained integration tests..."
echo "   These tests will:"
echo "   - Create test databases and pages in your Notion workspace"
echo "   - Verify create/read/update operations work correctly"
echo "   - Clean up by archiving all test data"
echo ""

./gradlew test --tests "*SelfContainedIntegrationTest*" --info

# Check if tests passed
if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 All integration tests passed!"
    echo "   Your Notion API integration is working correctly"
    echo "   Test data has been archived (not deleted)"
else
    echo ""
    echo "❌ Some integration tests failed"
    echo "   Check the output above for details"
    echo "   Common issues:"
    echo "   - Invalid API token or permissions"
    echo "   - Parent page ID doesn't exist or isn't accessible"
    echo "   - Network connectivity issues"
    exit 1
fi