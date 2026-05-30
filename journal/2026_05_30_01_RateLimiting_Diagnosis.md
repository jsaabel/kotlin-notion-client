# Rate Limiting Implementation — Diagnosis

**Date:** 2026-05-30
**Type:** Investigation / Diagnosis
**Status:** No code changes — assessment only
**Scope:** `ratelimit/NotionRateLimit.kt`, `ratelimit/BackoffCalculator.kt`, `ratelimit/RateLimitModels.kt`, and all `api/*Api.kt` call sites

## TL;DR

The implementation is **reactive-only and partially wired**. It catches 429s and applies
exponential backoff with jitter, but it ignores the response headers it goes to great
lengths to model, has dead code paths, has a "ghost" final delay, and provides zero
concurrency control — so a parallel batch will fail loudly under load rather than smoothly
throttle.

## Architecture summary

- `NotionRateLimit` is shaped like a Ktor plugin (`HttpClientPlugin<Config, NotionRateLimit>`)
  but its `install` method only stores the instance in `httpClient.attributes`.
  **It does not hook the request pipeline at all.**
- Every API method must opt in by wrapping its request in
  `httpClient.executeWithRateLimit { … }` (`NotionRateLimit.kt:172`).
- `executeWithRateLimit` only fires when the wrapped lambda throws
  `NotionException.ApiError` with `status == 429`. Anything else re-throws immediately.
- `BackoffCalculator` decides delay and whether to retry; uses `baseDelay * 2^attempt`,
  applies a strategy multiplier (CONSERVATIVE 1.5×, AGGRESSIVE 0.7×, BALANCED 1×),
  adds ±`jitterFactor` jitter, caps at `maxDelayMs`.

## Scenario walk-throughs

### 1. Rapid synchronous calls (single coroutine, e.g. fetching 50 pages in a loop)

- No proactive throttle — calls go out as fast as the network allows.
- Notion's ~3 req/s ceiling kicks in around request 4–5; 429s start.
- Each 429 triggers retry with BALANCED defaults (1 s, 2 s, 4 s, 8 s).
- The single-coroutine case is roughly fine in practice: the latency added by waits
  *acts* as throttling for the rest of the run.
- **Bug**: with `maxRetries=3` and persistent failure, the loop calculates and applies
  a delay (~8 s) for `attemptNumber==3` and then exits without retrying — a wasted sleep
  at the end of every failed chain (`NotionRateLimit.kt:106-153`).

### 2. Large batch of asynchronous calls

`coroutineScope { items.map { async { client.pages.retrieve(it) } } }` is where the
implementation is most broken.

- 100 coroutines fire simultaneously; ~3 succeed, ~97 get 429.
- All 97 enter their own retry loop with the same exponential schedule. Jitter is only
  ±10 %, so they cluster within a 200 ms window.
- After ~1 s they all retry simultaneously → ~3 succeed, ~94 fail again.
- After 1 + 2 + 4 = 7 s (≈3 retries each), only ~9 of the 100 will have succeeded.
  The other ~91 hit `MAX_RETRIES_EXCEEDED` and throw, even though the right answer was
  simply "wait longer."
- There is no global semaphore, token bucket, or shared queue — every coroutine reasons
  about rate limits in isolation.
- **Effect**: batch operations larger than ~`(maxRetries+1) × 3` items will reliably
  throw, not slowly succeed.

### 3. Notion sends `Retry-After: N`

- The model `RateLimitState.fromHeaders` exists and is unit-tested, and
  `BackoffCalculator.calculateDelay` honors `retryAfterSeconds` when given a state.
- **But**: `executeWithRetry` always passes `null` for `rateLimitState`
  (`NotionRateLimit.kt:119`). The response headers are never read on a 429.
- Result: `respectRetryAfter` is effectively dead config. If Notion says "wait 30 s"
  we still wait 1 s; if it says "wait 1 s" we still wait 1 s, then 2 s, then 4 s.

### 4. 5xx server errors or network errors

- `executeWithRetry` only retries on `status == 429`. Any other `NotionException.ApiError`
  (500, 502, 503, 504) and any non-`ApiError` exception (timeouts, connection resets)
  re-throws immediately.
- The `isServerError` / `isNetworkError` branches in `BackoffCalculator.isRetryableError`
  (`BackoffCalculator.kt:168-200`) are **dead code from the plugin's perspective**;
  they only run through unit tests calling `shouldRetry` directly.

### 5. Mixed-status errors and the string-matching classifier

- `isRetryableError` / `isClientError` / `isServerError` work by
  `error.message?.lowercase().contains("429" / "400" / "500" / …)`.
  Brittle: a 404 whose response body or URL contains "500" would be misclassified.
  Notion's JSON also uses `"code": "rate_limited"` — if we ever surface that string
  instead of `429`, classification breaks.
- Currently saved by the fact that `NotionException.ApiError.message` always embeds
  `(HTTP $status)`.

### 6. Endpoints not wrapped

- `SearchApi` calls `httpClient.post("/v1/search")` directly with **no
  `executeWithRateLimit` and no try/catch** (`SearchApi.kt:71-76`). Search 429s blow
  up immediately and surface as raw Ktor exceptions, not `NotionException`.
- `FileUploadApi` similarly calls `client.post/get/submitFormWithBinaryData` directly —
  uploads are not protected by the plugin.
- `EnhancedFileUploadApi` has its **own** `withRetry` + `RetryConfig` system in
  `models.files`, parallel to `NotionRateLimit`. Two retry frameworks, neither aware
  of the other.

### 7. Pagination flows

- Each page fetch is its own `executeWithRateLimit` call, so a 1 000-page cursor walk
  hammers Notion sequentially with no proactive pacing. In practice the sequential
  latency keeps it under 3 req/s, but a `Flow` consumed by a fast collector will get
  a 429 mid-walk and rely on reactive backoff.

## Concrete defects, ranked

1. **No global concurrency control** — parallel batches will fail with
   `MAX_RETRIES_EXCEEDED` instead of throttling. Highest user-visible impact.
2. **`Retry-After` and `x-ratelimit-*` headers are never read** at runtime;
   `RateLimitState` is built only by tests. The config flag `respectRetryAfter` is a no-op.
3. **5xx and network errors aren't retried** despite `BackoffCalculator` claiming to
   handle them. Misleading API surface.
4. **`SearchApi` and `FileUploadApi` bypass the rate limiter entirely.** Coverage is
   incomplete and silent.
5. **Wasted final delay**: after the last failed retry, the loop sleeps the next computed
   backoff (up to 8 s on BALANCED) before throwing.
6. **String-matching error classifier** is brittle; should use the typed `status` field
   of `NotionException.ApiError`.
7. **Two parallel retry systems** (`NotionRateLimit` vs `EnhancedFileUploadApi.withRetry`
   + `RetryConfig`) with overlapping responsibilities and no shared config.
8. The `RateLimitDecision.Proceed` branch is unreachable — `shouldRetry` only ever
   returns `Wait` or `Reject`.
9. Off-by-one risk in `maxRetries` semantics: with `maxRetries=3` the code performs
   4 HTTP attempts (initial + 3). Reasonable but worth documenting; users likely
   expect 3 total.

## Suggested investigation next steps (not implementation)

- Decide whether the plugin should become a real Ktor pipeline interceptor (so headers
  can be read on the response and 5xx / network errors can be retried in one place)
  or stay an opt-in wrapper.
- Decide on a concurrency model: global semaphore sized to Notion's ~3 req/s ceiling,
  or a token bucket per `HttpClient`. This is the single biggest improvement for batch
  workloads.
- Unify with `EnhancedFileUploadApi.withRetry` so there is one retry config.
- Cover the gap in `SearchApi` / `FileUploadApi`.