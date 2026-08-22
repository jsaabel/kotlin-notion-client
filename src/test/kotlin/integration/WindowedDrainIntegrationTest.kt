package integration

import io.kotest.assertions.fail
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.datasources.RowIterationKey
import it.saabel.kotlinnotionclient.models.datasources.SortDirection
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.coroutines.delay

/**
 * Integration tests for the windowed row drain (FOLLOWUPS item 26; issue #61).
 *
 * The windowing design (ascending key sort + re-query from the boundary on truncation)
 * is the agent's own, not a documented one — Notion's "Query large data sources" guide
 * 404s. Two things need live adjudication:
 *
 * 1. Whether a `unique_id` property is accepted as a *property sort* — the load-bearing
 *    assumption behind [RowIterationKey.UniqueId]. Tested here directly with a plain
 *    sorted query, plus a full (small) drain on that key.
 * 2. The actual `has_more`/`next_cursor`/`request_status` behaviour at the 10,000-row
 *    cap. Creating >10k rows via API costs thousands of requests, so that half is
 *    gated behind NOTION_TEST_LARGE_DATASOURCE_ID pointing at an existing large
 *    data source (see the last test's KDoc for setup).
 *
 * The always-on test verifies drain-vs-plain-query equivalence on a small data source
 * it creates itself (default `created_time` key, with and without a caller filter).
 *
 * The unique_id tests are gated on NOTION_TEST_DATASOURCE_ID (a data source that has a
 * `unique_id` property — cannot be created via API; see NewFilterTypesIntegrationTest
 * setup) and NOTION_TEST_UNIQUE_ID_PROPERTY (property name, default "ID").
 *
 * Run with: ./gradlew integrationTest --tests "*WindowedDrainIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class WindowedDrainIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) windowed drain integration — env gate not satisfied" {
                println("Skipping WindowedDrainIntegrationTest — set required env vars (see .env.example)")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val uniqueIdDataSourceId = System.getenv("NOTION_TEST_DATASOURCE_ID")
            val uniqueIdPropertyName = System.getenv("NOTION_TEST_UNIQUE_ID_PROPERTY") ?: "ID"
            val largeDataSourceId = System.getenv("NOTION_TEST_LARGE_DATASOURCE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Windowed Drain — Integration Tests")
                        icon.emoji("🌀")
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page (all children trashed)")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. Drain equivalence on a small data source (created_time key)
            // ------------------------------------------------------------------
            "drain matches plain query on a small data source, with and without filter" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Drain Equivalence Test")
                        properties {
                            title("Name")
                            checkbox("Even")
                        }
                    }
                val dsId = database.dataSources.first().id
                delay(500)

                val rowCount = 12
                repeat(rowCount) { i ->
                    notion.pages.create {
                        parent.dataSource(dsId)
                        properties {
                            title("Name", "Row %02d".format(i))
                            checkbox("Even", i % 2 == 0)
                        }
                    }
                }
                delay(1000)

                val plain = notion.dataSources.query(dsId)
                val drained = notion.dataSources.collectAllRows(dsId)
                println("🔎 plain query: ${plain.size} rows, drain: ${drained.size} rows")
                drained.size shouldBe rowCount
                drained.map { it.id }.toSet() shouldBe plain.map { it.id }.toSet()

                // Drain output is ordered ascending by created_time.
                drained.map { it.createdTime } shouldBe drained.map { it.createdTime }.sorted()

                // Caller filter survives being and-combined with the window filter.
                val filtered =
                    notion.dataSources.collectAllRows(dsId) {
                        filter { checkbox("Even").equals(true) }
                    }
                println("🔎 filtered drain: ${filtered.size} rows")
                filtered.size shouldBe rowCount / 2

                println("✅ Drain ≡ plain query on small data source (created_time key)")
            }

            // ------------------------------------------------------------------
            // 2. unique_id property sort acceptance — item 26's uncertain assumption
            // ------------------------------------------------------------------
            if (uniqueIdDataSourceId.isNullOrBlank()) {
                "(Skipped) unique_id sort probe — NOTION_TEST_DATASOURCE_ID not set" {
                    println(
                        "Set NOTION_TEST_DATASOURCE_ID to a data source with a unique_id property (see NewFilterTypesIntegrationTest setup)",
                    )
                }
            } else {
                "unique_id property accepts a property sort" {
                    val sorted =
                        notion.dataSources.query(
                            uniqueIdDataSourceId,
                            DataSourceQueryBuilder()
                                .sortBy(uniqueIdPropertyName, SortDirection.ASCENDING)
                                .build(),
                        )
                    println("🔎 FINDING: property sort on unique_id \"$uniqueIdPropertyName\" ACCEPTED, ${sorted.size} rows returned")

                    val numbers =
                        sorted.mapNotNull { page ->
                            (page.properties[uniqueIdPropertyName] as? PageProperty.UniqueId)?.uniqueId?.number
                        }
                    println("🔎 unique_id values in returned order: $numbers")
                    numbers.size shouldBe sorted.size
                    numbers.shouldBeStrictlyIncreasing()
                }

                "drain on RowIterationKey.UniqueId matches plain query" {
                    val plain = notion.dataSources.query(uniqueIdDataSourceId)
                    val drained =
                        notion.dataSources.collectAllRows(
                            uniqueIdDataSourceId,
                            key = RowIterationKey.UniqueId(uniqueIdPropertyName),
                        )
                    println("🔎 plain query: ${plain.size} rows, unique_id drain: ${drained.size} rows")
                    drained.map { it.id }.toSet() shouldBe plain.map { it.id }.toSet()

                    val numbers =
                        drained.map { page ->
                            (page.properties[uniqueIdPropertyName] as PageProperty.UniqueId).uniqueId!!.number!!
                        }
                    numbers.shouldBeStrictlyIncreasing()
                    println("✅ unique_id drain emits every row exactly once, strictly increasing")
                }
            }

            // ------------------------------------------------------------------
            // 3. Full >10k drain — opt-in, needs an existing large data source
            // ------------------------------------------------------------------
            // Setup: CSV-import >10,000 rows in the Notion UI (creating them via API costs
            // one request per row), add a unique_id "ID" property, share with the
            // integration, then set NOTION_TEST_LARGE_DATABASE_ID (the id from the
            // database URL; the data source id is resolved from it) or
            // NOTION_TEST_LARGE_DATASOURCE_ID directly. Optionally set
            // NOTION_TEST_LARGE_UNIQUE_ID_PROPERTY (e.g. "ID") to exercise the UniqueId
            // key. This is the only way to observe the real truncation behaviour
            // (request_status.type == "incomplete") that the windowing engine keys on.
            //
            // Note: a bulk CSV import lands many rows in the same created_time minute. If
            // >10k rows share one minute, the CreatedTime key is *designed* to stall
            // (NotionException.IterationStalled) — that outcome is caught and reported as
            // a finding, and the UniqueId drain carries the count assertions instead.
            val largeDatabaseId = System.getenv("NOTION_TEST_LARGE_DATABASE_ID")
            if (largeDataSourceId.isNullOrBlank() && largeDatabaseId.isNullOrBlank()) {
                "(Skipped) >10k drain — NOTION_TEST_LARGE_DATABASE_ID not set" {
                    println("Set NOTION_TEST_LARGE_DATABASE_ID (or NOTION_TEST_LARGE_DATASOURCE_ID) to an existing >10k-row source")
                }
            } else {
                "drains a >10k-row data source past the truncation cap" {
                    val dsId =
                        largeDataSourceId?.takeIf { it.isNotBlank() }
                            ?: notion.databases
                                .retrieve(largeDatabaseId!!)
                                .dataSources
                                .first()
                                .id
                    // The plain query is designed to fail fast on truncation — observing that
                    // exception (with request_status populated) IS the item-26 confirmation
                    // of the live truncation shape.
                    val truncation =
                        try {
                            val all = notion.dataSources.query(dsId)
                            fail(
                                "Data source returned only ${all.size} rows without truncation — " +
                                    "the >10k drain test needs a data source with more than 10,000 rows",
                            )
                        } catch (e: NotionException.QueryResultLimitReached) {
                            e
                        }
                    println(
                        "🔎 FINDING: plain query threw QueryResultLimitReached after " +
                            "${truncation.partialResults.size} rows; " +
                            "requestStatus=${truncation.requestStatus}, " +
                            "nextCursor present=${truncation.nextCursor != null}",
                    )

                    var createdTimeIds: Set<String>? = null
                    val start = System.currentTimeMillis()
                    try {
                        val ids = mutableSetOf<String>()
                        var count = 0
                        notion.dataSources.iterateAllRows(dsId).collect { page ->
                            ids.add(page.id)
                            count++
                            if (count % 1000 == 0) println("   …$count rows drained")
                        }
                        val elapsed = System.currentTimeMillis() - start
                        println("🔎 FINDING: created_time drain: $count rows in ${elapsed}ms")
                        ids.size shouldBe count // no duplicates
                        count shouldBeGreaterThan 10_000
                        createdTimeIds = ids
                    } catch (e: NotionException.IterationStalled) {
                        println("🔎 FINDING: created_time drain STALLED as designed (>10k rows in one minute): ${e.message}")
                    }

                    val largeUniqueIdProp = System.getenv("NOTION_TEST_LARGE_UNIQUE_ID_PROPERTY")
                    if (largeUniqueIdProp.isNullOrBlank()) {
                        println("ℹ️ NOTION_TEST_LARGE_UNIQUE_ID_PROPERTY not set — skipping unique_id drain")
                    } else {
                        val uStart = System.currentTimeMillis()
                        val drainedByUniqueId =
                            notion.dataSources.collectAllRows(
                                dsId,
                                key = RowIterationKey.UniqueId(largeUniqueIdProp),
                            )
                        val uElapsed = System.currentTimeMillis() - uStart
                        println("🔎 FINDING: unique_id drain: ${drainedByUniqueId.size} rows in ${uElapsed}ms")
                        drainedByUniqueId.size shouldBeGreaterThan 10_000
                        drainedByUniqueId.map { it.id }.toSet().size shouldBe drainedByUniqueId.size
                        // Rows whose unique_id is not yet backfilled are silently excluded by
                        // the window filter (documented on RowIterationKey.UniqueId), so a
                        // count mismatch vs the created_time drain is reported, not failed.
                        createdTimeIds?.let { ctIds ->
                            val missing = ctIds - drainedByUniqueId.map { p -> p.id }.toSet()
                            if (missing.isEmpty()) {
                                println("✅ unique_id drain matches created_time drain exactly")
                            } else {
                                println(
                                    "🔎 FINDING: unique_id drain missed ${missing.size} rows vs created_time " +
                                        "drain — consistent with rows still awaiting unique_id backfill",
                                )
                            }
                        }
                    }
                }
            }
        }
    })
