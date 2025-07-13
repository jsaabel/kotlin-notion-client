# Rate Limiting Test Guide

This guide explains how to manually test the rate limiting functionality against the live Notion API.

## Purpose

The manual rate limiting test (`RateLimitLiveTest`) is designed to:
- Verify that our rate limiting mechanism correctly handles HTTP 429 responses
- Ensure automatic retry with exponential backoff works as expected
- Confirm that rate limiting is coordinated across all API endpoints
- Test real-world scenarios with burst traffic

## Prerequisites

1. A valid Notion API token
2. A test page ID that your token has access to
3. Understanding that this test will make many API requests

## Running the Test

### Option 1: Using the Test Script

```bash
# Set environment variables
export NOTION_API_TOKEN="your-secret-token"
export NOTION_TEST_PAGE_ID="your-page-id"

# Run the test script
./test-rate-limiting.sh
```

### Option 2: Direct Gradle Command

```bash
export NOTION_API_TOKEN="your-secret-token"
export NOTION_TEST_PAGE_ID="your-page-id"

# Using the custom manual test task
./gradlew manualTest

# Or run a specific test class
./gradlew manualTest --tests "*RateLimitLiveTest*"
```

## What to Expect

### Test 1: Concurrent Burst Test
- Makes 30 concurrent requests to exceed Notion's burst capacity
- You should see some requests succeed immediately
- After the burst limit, you'll see rate limiting kick in
- Requests will be retried with delays
- All requests should eventually succeed

Example output:
```
✅ Request 1 succeeded (59833787-2cf9-4fdf-8782-e53db20768a5)
✅ Request 2 succeeded (59833787-2cf9-4fdf-8782-e53db20768a5)
⏳ Request 15 was rate limited
   Request 15 took 2584ms
```

### Test 2: Sustained Load Test
- Makes rapid sequential requests until rate limiting is triggered
- Shows exactly when rate limiting kicks in
- Demonstrates the retry mechanism in action

### Test 3: Header Capture Test
- Makes requests with no retry to see raw 429 responses
- Verifies we're correctly detecting rate limit errors

## Interpreting Results

### Success Indicators
- ✅ All requests eventually succeed (with retries)
- ⏳ Some requests take >1 second (due to retry delays)
- 📊 Clear before/after timing differences

### Warning Signs
- ❌ Unhandled failures (not 429 errors)
- ⚠️ No rate limiting detected (may need more requests)

## Important Notes

1. **Burst Tolerance**: Notion tolerates short bursts, so we use 30+ requests
2. **Timing**: The test may take 30-120 seconds depending on retry delays

## Rate Limiting Behavior

Our implementation:
- **Does NOT throttle preemptively** - requests run at full speed initially
- **Only activates on 429 responses** - reactive, not proactive
- **Automatically retries** - with exponential backoff (1s, 2s, 4s, 8s...)
- **Respects Retry-After headers** - waits the exact duration Notion requests
- **Applies globally** - rate limits are shared across all endpoints

## Troubleshooting

### No Rate Limiting Triggered
- Increase the number of concurrent requests in the test
- Ensure you're using a valid API token and page ID
- Check if Notion has increased rate limits for your integration

### Too Many Failures
- Check your API token has access to the test page
- Verify the page ID is correct
- Ensure you have a stable internet connection

### Test Timeout
- The test has a 120-second timeout
- If it times out, reduce maxRetries in the test config