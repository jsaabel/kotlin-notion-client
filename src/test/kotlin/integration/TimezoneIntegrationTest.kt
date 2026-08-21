package integration

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.DateObject
import it.saabel.kotlinnotionclient.models.base.Mention
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.pages.DateData
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Integration test for timezone round-trip correctness.
 *
 * Tests all paths by which date/datetime values with timezone information reach
 * the Notion API, and verifies what is preserved on read-back.
 *
 * Key findings from live API:
 * - Notion NEVER preserves the named time_zone field in responses — it always converts
 *   the datetime to a numeric offset (e.g. -04:00) and returns time_zone=null.
 * - Notion treats datetime strings as local time and applies the UTC offset of the named
 *   timezone at that date to produce the stored representation.
 * - Sending a UTC instant (e.g. "2026-06-15T18:30:00Z") with time_zone="America/New_York"
 *   causes Notion to misinterpret the time — it strips the Z and applies the NY offset,
 *   returning "2026-06-15T18:30:00.000-04:00" instead of the correct "14:30-04:00".
 *   This is why the LocalDateTime + TimeZone builders never pair an instant with a
 *   time_zone — they resolve the zone's offset locally and send the wall clock with
 *   that offset (and no time_zone field).
 *
 * Part 3 — naive datetimes and DST boundaries (scenarios N1-N6):
 *
 * Measured against the live API on 2026-08-21; the values below are what Notion
 * actually returned, not what the documentation implies. These cover the case that
 * broke a downstream consumer: writing an offset-less datetime with no time_zone
 * field, where the wall-clock time still displayed as sent but the underlying instant
 * had silently moved by the local UTC offset.
 *
 * - A naive datetime with no time_zone is read as UTC. "2026-06-15T14:30:00" comes back
 *   as "2026-06-15T14:30:00.000+00:00", and the December equivalent comes back as
 *   "…+00:00" too (N1/N2) — the offset does not track the season, so Notion is not
 *   resolving naive input against a DST-aware workspace zone. This was previously only
 *   *inferred* from the downstream symptom; it is now measured. Consequence: an
 *   offset-less write preserves the wall clock and silently reassigns the instant. A
 *   caller who means 14:30 in Oslo must send an offset or the time_zone field — never a
 *   bare local datetime.
 * - Notion always answers with an explicit numeric offset, never "Z". UTC comes back as
 *   "+00:00", always with a ".000" fraction (N1/N2).
 * - The named-zone path (the scenario D shape: naive local + time_zone) resolves
 *   per-instant, not per-day. On 2026-10-25, Europe/Oslo 01:00 returns +02:00 and 04:00
 *   returns +01:00 — same calendar day, either side of the 03:00 local changeover (N3/N4).
 * - An ambiguous local time resolves to the EARLIER of its two instants. Europe/Oslo
 *   2026-10-25T02:30 occurs at both +02:00 (00:30Z) and +01:00 (01:30Z); Notion returns
 *   "2026-10-25T02:30:00.000+02:00" (N5). Callers cannot express the later one.
 * - A nonexistent local time is shifted FORWARD by the gap, moving the wall clock rather
 *   than the offset. Europe/Oslo 2026-03-29T02:30 does not exist; Notion returns
 *   "2026-03-29T03:30:00.000+02:00" = 01:30Z (N6). Returning 02:30+01:00 would have been
 *   the same instant rendered differently — Notion does not do that.
 * - N3-N6 agree exactly with what the IANA tz database (via java.time) makes of the same
 *   local times, ambiguity and gap resolution included. Nothing observed so far suggests
 *   Notion's zone resolution diverges from tzdb.
 * - Nothing in this round contradicted the assumptions the test went in with. A failing
 *   Part 3 assertion therefore means Notion's behaviour has CHANGED — record the new
 *   behaviour here before touching the expectation.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew test --tests "*TimezoneIntegrationTest"
 * (There is no separate `integrationTest` task — integration specs live in the normal
 * test task and skip themselves unless the env vars above are set. Other specs in this
 * package still name the old task in their KDoc.)
 */
@Tags("Integration", "RequiresApi")
class TimezoneIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) timezone round-trip integration" {
                println("Skipping TimezoneIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Timezone Round-Trip — Integration Tests")
                        icon.emoji("🕐")
                        content {
                            callout(
                                "ℹ️",
                                "Tests date/datetime round-trips across all timezone paths. " +
                                    "Part 1: database date properties (plain date, UTC instant, " +
                                    "named TZ via builder, explicit time_zone field, date range). " +
                                    "Part 2: rich text date mentions (same paths). " +
                                    "Part 3: naive datetimes with no time_zone, and Europe/Oslo DST " +
                                    "boundaries including the ambiguous and nonexistent local times. " +
                                    "Each scenario is shown with sent and received values side by side.",
                            )
                        }
                    }
                containerPageId = container.id
                println("Container page: ${container.url}")
                delay(500.milliseconds)
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("🧹 Container page trashed")
                } else {
                    println("🔧 Cleanup skipped — container: https://notion.so/${containerPageId.replace("-", "")}")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. Database property round-trips
            // ------------------------------------------------------------------
            "should round-trip database date properties across timezone paths" {
                // Database has a "Sent Value" text column alongside the date column so
                // each row shows what was sent and the resulting date chip side by side.
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Date Property Round-Trip")
                        icon.emoji("📅")
                        properties {
                            title("Scenario")
                            richText("Sent Value")
                            date("Date Prop")
                        }
                    }
                delay(1000.milliseconds)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()

                // ── A: LocalDate ─────────────────────────────────────────────
                val localDateExpected = LocalDate(2026, 6, 15)
                val localDatePage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Scenario", "A: LocalDate")
                            richText("Sent Value", "start=$localDateExpected, tz=null")
                            date("Date Prop", localDateExpected)
                        }
                    }

                // ── B: LocalDateTime + UTC ────────────────────────────────────
                val localDateTimeUtcExpected = LocalDateTime(2026, 6, 15, 14, 30)
                val localDateTimeUtcPage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Scenario", "B: LocalDateTime UTC")
                            richText("Sent Value", "start=$localDateTimeUtcExpected+00:00 (offset-bearing), tz=null")
                            dateTime("Date Prop", localDateTimeUtcExpected, TimeZone.UTC)
                        }
                    }

                // ── C: LocalDateTime + named TZ (offset-preserving) ───────────
                // The dateTime() builder resolves the zone's offset at the value's own
                // local date and sends the wall clock with that offset — it does NOT
                // convert to a UTC instant.
                val namedTz = TimeZone.of("America/New_York")
                val localDateTimeNyExpected = LocalDateTime(2026, 6, 15, 14, 30)
                val localDateTimeNyPage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Scenario", "C: LocalDateTime named TZ (builder → wall clock + offset)")
                            richText("Sent Value", "start=$localDateTimeNyExpected-04:00 (offset resolved locally), tz=null")
                            dateTime("Date Prop", localDateTimeNyExpected, namedTz)
                        }
                    }

                // ── D: Explicit time_zone field via dateTimeWithTimeZone ───────
                // Notion converts the naive local time + named TZ to an offset timestamp,
                // then drops the named TZ from the response (returns time_zone=null).
                val explicitTzDatetime = "2026-06-15T14:30:00"
                val explicitTzName = "America/New_York"
                val explicitTzPage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Scenario", "D: Explicit time_zone field")
                            richText("Sent Value", "start=$explicitTzDatetime, tz=$explicitTzName")
                            dateTimeWithTimeZone("Date Prop", explicitTzDatetime, explicitTzName)
                        }
                    }

                // ── E: Date range ─────────────────────────────────────────────
                val rangeStart = LocalDate(2026, 6, 15)
                val rangeEnd = LocalDate(2026, 6, 20)
                val dateRangePage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Scenario", "E: Date range")
                            richText("Sent Value", "start=$rangeStart, end=$rangeEnd, tz=null")
                            dateRange("Date Prop", rangeStart, rangeEnd)
                        }
                    }

                delay(1000.milliseconds)

                // ── Read back ─────────────────────────────────────────────────
                val readA = notion.pages.retrieve(localDatePage.id)
                val readB = notion.pages.retrieve(localDateTimeUtcPage.id)
                val readC = notion.pages.retrieve(localDateTimeNyPage.id)
                val readD = notion.pages.retrieve(explicitTzPage.id)
                val readE = notion.pages.retrieve(dateRangePage.id)

                fun PageProperty?.asDateData(): DateData? = (this as? PageProperty.Date)?.date

                val actualA = readA.properties["Date Prop"].asDateData()
                val actualB = readB.properties["Date Prop"].asDateData()
                val actualC = readC.properties["Date Prop"].asDateData()
                val actualD = readD.properties["Date Prop"].asDateData()
                val actualE = readE.properties["Date Prop"].asDateData()

                // ── Report on container page ──────────────────────────────────
                fun passOrFail(match: Boolean) = if (match) "✅" else "❌"

                val aOk = actualA?.start == localDateExpected.toString() && actualA.timeZone == null
                val bOk = actualB?.start?.startsWith("2026-06-15T14:30:00") == true && actualB.timeZone == null
                // C: wall clock preserved with the NY offset (UTC-4 in June → 14:30-04:00)
                val cOk = actualC?.start?.startsWith("2026-06-15T14:30:00") == true && actualC.timeZone == null
                // D: Notion applies the UTC offset and drops the named TZ from response
                val dOk = actualD?.start?.startsWith("2026-06-15T14:30:00") == true && actualD.timeZone == null
                val eOk =
                    actualE?.start == rangeStart.toString() &&
                        actualE.end == rangeEnd.toString() &&
                        actualE.timeZone == null

                notion.blocks.appendChildren(containerPageId) {
                    heading2("Part 1: Database Date Properties")
                    paragraph("Each row in the database above has a 'Sent Value' column alongside the date chip.")

                    paragraph {
                        text("A: LocalDate  ")
                        bold("sent:")
                        text(" start=$localDateExpected, tz=null  ")
                        bold("got:")
                        text(" start=${actualA?.start}, tz=${actualA?.timeZone}  ${passOrFail(aOk)}")
                    }
                    paragraph {
                        text("B: LocalDateTime UTC  ")
                        bold("sent:")
                        text(" start=$localDateTimeUtcExpected (as instant), tz=null  ")
                        bold("got:")
                        text(" start=${actualB?.start}, tz=${actualB?.timeZone}  ${passOrFail(bOk)}")
                    }
                    paragraph {
                        text("C: LocalDateTime named TZ via builder — ")
                        italic("wall clock preserved, offset resolved locally, time_zone never sent")
                        text("  ")
                        bold("sent:")
                        text(" start=$localDateTimeNyExpected-04:00, tz=null  ")
                        bold("got:")
                        text(" start=${actualC?.start}, tz=${actualC?.timeZone}  ${passOrFail(cOk)}")
                    }
                    paragraph {
                        text("D: Explicit time_zone field — ")
                        italic("Notion applies offset and drops named TZ from response")
                        text("  ")
                        bold("sent:")
                        text(" start=$explicitTzDatetime, tz=$explicitTzName  ")
                        bold("got:")
                        text(" start=${actualD?.start}, tz=${actualD?.timeZone}  ${passOrFail(dOk)}")
                    }
                    paragraph {
                        text("E: Date range  ")
                        bold("sent:")
                        text(" start=$rangeStart, end=$rangeEnd, tz=null  ")
                        bold("got:")
                        text(" start=${actualE?.start}, end=${actualE?.end}, tz=${actualE?.timeZone}  ${passOrFail(eOk)}")
                    }
                }

                // ── Assertions ────────────────────────────────────────────────
                println("\n=== Database date property round-trip ===")
                println("A  start=${actualA?.start}  tz=${actualA?.timeZone}")
                println("B  start=${actualB?.start}  tz=${actualB?.timeZone}")
                println("C  start=${actualC?.start}  tz=${actualC?.timeZone}")
                println("D  start=${actualD?.start}  tz=${actualD?.timeZone}")
                println("E  start=${actualE?.start}  end=${actualE?.end}  tz=${actualE?.timeZone}")

                actualA?.start shouldBe localDateExpected.toString()
                actualA?.timeZone shouldBe null

                actualB?.start?.startsWith("2026-06-15T14:30:00") shouldBe true
                actualB?.timeZone shouldBe null

                // C: builder preserves the wall clock and sends the NY offset — Notion
                // stores it as sent (NY is UTC-4 in June → 14:30-04:00)
                actualC?.start?.startsWith("2026-06-15T14:30:00") shouldBe true
                actualC?.timeZone shouldBe null

                // D: Notion converts naive local+TZ to offset string, drops named TZ
                actualD?.start?.startsWith("2026-06-15T14:30:00") shouldBe true
                actualD?.timeZone shouldBe null

                actualE?.start shouldBe rangeStart.toString()
                actualE?.end shouldBe rangeEnd.toString()
                actualE?.timeZone shouldBe null

                println("✅ Database date property round-trip verified")
            }

            // ------------------------------------------------------------------
            // 2. Rich text date mention round-trips
            // ------------------------------------------------------------------
            "should round-trip rich text date mentions across timezone paths" {
                val mentionPage =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Rich Text Date Mentions — Timezone Round-Trip")
                        icon.emoji("📝")
                        content {
                            callout(
                                "ℹ️",
                                "Each paragraph below shows the sent values alongside the Notion date chip, " +
                                    "followed immediately by the received values and match result.",
                            )
                        }
                    }
                val mentionPageId = mentionPage.id
                delay(500.milliseconds)

                // Scenario values
                val dt1Start = "2026-06-15"
                val dt2Start = "2026-06-15T14:30:00"
                val dt2Tz = "America/New_York"
                val dt3Date = LocalDate(2026, 6, 15)
                val dt4Local = LocalDateTime(2026, 6, 15, 14, 30)
                val dt4Tz = TimeZone.of("America/New_York")
                val dt5Instant = Instant.parse("2026-06-15T18:30:00Z")

                // Write 5 mention paragraphs so Notion processes them.
                // Each paragraph contains a brief label + the date chip so the chip
                // is visible alongside the comparison that will be appended below.
                notion.blocks.appendChildren(mentionPageId) {
                    paragraph {
                        text("M1 (plain string date, no tz)  ")
                        dateMention(dt1Start)
                    }
                    paragraph {
                        text("M2 (string datetime + explicit tz=$dt2Tz)  ")
                        dateMention(dt2Start, timeZone = dt2Tz)
                    }
                    paragraph {
                        text("M3 (LocalDate)  ")
                        dateMention(dt3Date)
                    }
                    paragraph {
                        text("M4 (LocalDateTime $dt4Local + tz=$dt4Tz)  ")
                        dateMention(dt4Local, timeZone = dt4Tz)
                    }
                    paragraph {
                        text("M5 (Instant $dt5Instant, no tz)  ")
                        dateMention(dt5Instant)
                    }
                }
                delay(1000.milliseconds)

                // Read back the 5 mention blocks
                val blocks = notion.blocks.retrieveChildren(mentionPageId)

                fun Block.dateObject(): DateObject? {
                    val richText =
                        when (this) {
                            is Block.Paragraph -> paragraph.richText
                            else -> return null
                        }
                    return richText
                        .mapNotNull { it.mention }
                        .filterIsInstance<Mention.Date>()
                        .firstOrNull()
                        ?.date
                }

                // blocks[0] is the callout, blocks[1..5] are the mention paragraphs
                val actualM1 = blocks[1].dateObject()
                val actualM2 = blocks[2].dateObject()
                val actualM3 = blocks[3].dateObject()
                val actualM4 = blocks[4].dateObject()
                val actualM5 = blocks[5].dateObject()

                // ── Append comparison directly to the mention page ────────────
                fun passOrFail(match: Boolean) = if (match) "✅" else "❌"

                val m1Ok = actualM1?.start == dt1Start && actualM1.timeZone == null
                val m2StartOk = actualM2?.start?.startsWith("2026-06-15T14:30:00") == true
                val m2TzOk = actualM2?.timeZone == null
                val m3Ok = actualM3?.start == dt3Date.toString() && actualM3.timeZone == null
                // M4 sends the wall clock with the locally-resolved offset (14:30-04:00);
                // Notion preserves it.
                val m4StartOk = actualM4?.start?.startsWith("2026-06-15T14:30:00") == true
                val m4TzOk = actualM4?.timeZone == null
                val m5Ok = actualM5?.start?.startsWith("2026-06-15T18:30:00") == true && actualM5.timeZone == null

                notion.blocks.appendChildren(mentionPageId) {
                    divider()
                    heading3("Received values (compared to sent)")

                    paragraph {
                        bold("M1:")
                        text(
                            " sent start=$dt1Start, tz=null  |  got start=${actualM1?.start}, tz=${actualM1?.timeZone}  ${passOrFail(
                                m1Ok,
                            )}",
                        )
                    }
                    paragraph {
                        bold("M2:")
                        text(" sent start=$dt2Start, tz=$dt2Tz  |  got start=${actualM2?.start}, tz=${actualM2?.timeZone}  ")
                        text("start: ${passOrFail(m2StartOk)}  tz (Notion drops it): ${passOrFail(m2TzOk)}")
                    }
                    paragraph {
                        bold("M3:")
                        text(
                            " sent start=$dt3Date, tz=null  |  got start=${actualM3?.start}, tz=${actualM3?.timeZone}  ${passOrFail(m3Ok)}",
                        )
                    }
                    paragraph {
                        bold("M4:")
                        text(
                            " sent start=$dt4Local with ${dt4Tz.id}'s offset (wall clock preserved), tz=null  " +
                                "|  got start=${actualM4?.start}, tz=${actualM4?.timeZone}  ",
                        )
                        text("start: ${passOrFail(m4StartOk)}  tz: ${passOrFail(m4TzOk)}")
                    }
                    paragraph {
                        bold("M5:")
                        text(
                            " sent start=$dt5Instant, tz=null  |  got start=${actualM5?.start}, tz=${actualM5?.timeZone}  ${passOrFail(
                                m5Ok,
                            )}",
                        )
                    }
                }

                // ── Add summary link on container page ────────────────────────
                notion.blocks.appendChildren(containerPageId) {
                    heading2("Part 2: Rich Text Date Mentions")
                    paragraph("See sub-page for chips and comparison side by side: ${mentionPage.url}")
                    paragraph {
                        text("M1 (plain date): ${passOrFail(m1Ok)}  |  ")
                        text("M2 (explicit tz, Notion drops it): start ${passOrFail(m2StartOk)}  |  ")
                        text("M3 (LocalDate): ${passOrFail(m3Ok)}  |  ")
                        text("M4 (LocalDateTime+tz, builder fix): start ${passOrFail(m4StartOk)}  |  ")
                        text("M5 (Instant): ${passOrFail(m5Ok)}")
                    }
                }

                // ── Assertions ────────────────────────────────────────────────
                println("\n=== Rich text date mention round-trip ===")
                println("M1  start=${actualM1?.start}  tz=${actualM1?.timeZone}")
                println("M2  start=${actualM2?.start}  tz=${actualM2?.timeZone}")
                println("M3  start=${actualM3?.start}  tz=${actualM3?.timeZone}")
                println("M4  start=${actualM4?.start}  tz=${actualM4?.timeZone}")
                println("M5  start=${actualM5?.start}  tz=${actualM5?.timeZone}")

                actualM1?.start shouldBe dt1Start
                actualM1?.timeZone shouldBe null

                // M2: Notion converts local+tz to offset string, drops named TZ
                actualM2?.start?.startsWith("2026-06-15T14:30:00") shouldBe true
                actualM2?.timeZone shouldBe null

                actualM3?.start shouldBe dt3Date.toString()
                actualM3?.timeZone shouldBe null

                // M4: builder sends the wall clock with the locally-resolved -04:00 offset
                // (no instant conversion); Notion preserves it → "2026-06-15T14:30:00.000-04:00"
                actualM4?.start?.startsWith("2026-06-15T14:30:00") shouldBe true
                actualM4?.timeZone shouldBe null

                actualM5?.start?.startsWith("2026-06-15T18:30:00") shouldBe true
                actualM5?.timeZone shouldBe null

                println("✅ Rich text date mention round-trip verified")
            }

            // ------------------------------------------------------------------
            // 3. Naive datetimes and DST boundaries (Europe/Oslo)
            // ------------------------------------------------------------------
            // These scenarios are empirical: the expectations below were recorded from
            // the live API on 2026-08-21, not derived from documentation. When one fails,
            // the clue prints what Notion actually returned — record that in the KDoc
            // above instead of quietly relaxing the assertion.
            "should record how Notion resolves naive datetimes and DST boundaries" {
                /** Local class describing one send → read-back experiment. */
                data class Scenario(
                    val key: String,
                    val label: String,
                    /** Exactly what goes into `date.start`. */
                    val sentStart: String,
                    /** Exactly what goes into `date.time_zone` (null = field omitted). */
                    val sentTimeZone: String?,
                    /** Recorded local part of the returned string (first 19 chars). */
                    val expectedLocal: String,
                    /** Recorded offset of the returned string, normalised to ±hh:mm. */
                    val expectedOffset: String,
                    /** The instant that rendering denotes. */
                    val expectedInstant: Instant,
                    /** What the live API did, and what a different answer would have meant. */
                    val finding: String,
                )

                val scenarios =
                    listOf(
                        // ── N1/N2: the shape that caused the downstream bug ───────
                        // A naive local datetime with no time_zone field at all.
                        // Two of them, six months apart: had Notion answered with a
                        // workspace-local zone rather than UTC, a European workspace
                        // would have returned +02:00 here and +01:00 in N2. Both came
                        // back +00:00, so naive input is UTC.
                        Scenario(
                            key = "N1",
                            label = "Naive datetime, no time_zone (summer)",
                            sentStart = "2026-06-15T14:30:00",
                            sentTimeZone = null,
                            expectedLocal = "2026-06-15T14:30:00",
                            expectedOffset = "+00:00",
                            expectedInstant = Instant.parse("2026-06-15T14:30:00Z"),
                            finding =
                                "Read as UTC: the wall clock is preserved and the instant is silently " +
                                    "reassigned. This is exactly what moved a downstream consumer's events. " +
                                    "A different offset would have meant the value is resolved against some " +
                                    "workspace/user zone, making every offset-less write workspace-dependent.",
                        ),
                        Scenario(
                            key = "N2",
                            label = "Naive datetime, no time_zone (winter)",
                            sentStart = "2026-12-15T14:30:00",
                            sentTimeZone = null,
                            expectedLocal = "2026-12-15T14:30:00",
                            expectedOffset = "+00:00",
                            expectedInstant = Instant.parse("2026-12-15T14:30:00Z"),
                            finding =
                                "Same +00:00 as N1, six months later, so the offset does not track the " +
                                    "season. Together with N1 this rules out a DST-aware workspace zone: " +
                                    "naive input is UTC, not workspace-local.",
                        ),
                        // ── N3/N4: either side of the autumn transition ───────────
                        // Europe/Oslo moves +02:00 → +01:00 at 03:00 local on 2026-10-25.
                        Scenario(
                            key = "N3",
                            label = "Europe/Oslo, one hour before the autumn transition",
                            sentStart = "2026-10-25T01:00:00",
                            sentTimeZone = "Europe/Oslo",
                            expectedLocal = "2026-10-25T01:00:00",
                            expectedOffset = "+02:00",
                            expectedInstant = Instant.parse("2026-10-24T23:00:00Z"),
                            finding =
                                "Unambiguous CEST, resolved to +02:00. The named-zone path (scenario D) " +
                                    "picks the summer offset on the summer side of the boundary.",
                        ),
                        Scenario(
                            key = "N4",
                            label = "Europe/Oslo, one hour after the autumn transition",
                            sentStart = "2026-10-25T04:00:00",
                            sentTimeZone = "Europe/Oslo",
                            expectedLocal = "2026-10-25T04:00:00",
                            expectedOffset = "+01:00",
                            expectedInstant = Instant.parse("2026-10-25T03:00:00Z"),
                            finding =
                                "Unambiguous CET, resolved to +01:00 on the same calendar day as N3 — so " +
                                    "the named zone is resolved per-instant, not per-day. A +02:00 answer " +
                                    "here would have meant per-day resolution.",
                        ),
                        // ── N5: the autumn overlap — 02:30 happens twice ──────────
                        Scenario(
                            key = "N5",
                            label = "Europe/Oslo, ambiguous local time (occurs twice)",
                            sentStart = "2026-10-25T02:30:00",
                            sentTimeZone = "Europe/Oslo",
                            expectedLocal = "2026-10-25T02:30:00",
                            expectedOffset = "+02:00",
                            expectedInstant = Instant.parse("2026-10-25T00:30:00Z"),
                            finding =
                                "02:30 exists twice on this date: once at +02:00 (00:30Z) and once at " +
                                    "+01:00 (01:30Z). Notion picks the earlier one, matching java.time. " +
                                    "The later one is equally legitimate and callers cannot ask for it, " +
                                    "which is why the choice has to be written down.",
                        ),
                        // ── N6: the spring gap — 02:30 never happens ──────────────
                        // Europe/Oslo jumps 02:00 → 03:00 local on 2026-03-29.
                        Scenario(
                            key = "N6",
                            label = "Europe/Oslo, nonexistent local time (spring gap)",
                            sentStart = "2026-03-29T02:30:00",
                            sentTimeZone = "Europe/Oslo",
                            expectedLocal = "2026-03-29T03:30:00",
                            expectedOffset = "+02:00",
                            expectedInstant = Instant.parse("2026-03-29T01:30:00Z"),
                            finding =
                                "02:30 does not exist on this date. Notion shifts the wall clock forward " +
                                    "by the gap (java.time's rule) → 03:30+02:00. It does not return " +
                                    "02:30+01:00, which would be the same instant rendered differently; " +
                                    "02:30+02:00 (00:30Z) would have been an hour earlier, so the instant " +
                                    "assertion is the one that carries the finding here.",
                        ),
                    )

                // ── Write one row per scenario ────────────────────────────────
                // Same reporting shape as Part 1: a "Sent Value" column next to the
                // date chip, plus the recorded value, so sent/expected/received line up.
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Naive Datetime & DST Boundary Round-Trip")
                        icon.emoji("🌍")
                        properties {
                            title("Scenario")
                            richText("Sent Value")
                            richText("Expected (recorded)")
                            date("Date Prop")
                        }
                    }
                delay(1000.milliseconds)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()

                val createdPages =
                    scenarios.map { scenario ->
                        scenario to
                            notion.pages.create {
                                parent.dataSource(ds.id)
                                properties {
                                    title("Scenario", "${scenario.key}: ${scenario.label}")
                                    richText(
                                        "Sent Value",
                                        "start=\"${scenario.sentStart}\", time_zone=${scenario.sentTimeZone ?: "(omitted)"}",
                                    )
                                    richText(
                                        "Expected (recorded)",
                                        "${scenario.expectedLocal}${scenario.expectedOffset} = ${scenario.expectedInstant}",
                                    )
                                    if (scenario.sentTimeZone == null) {
                                        // Raw property value → date.start only, no time_zone field.
                                        // The builder's dateTime() now rejects offset-less datetimes
                                        // (that guard exists because of what this scenario measures),
                                        // so the raw escape hatch is used to send it anyway.
                                        property(
                                            "Date Prop",
                                            PagePropertyValue.DateValue(date = DateData(start = scenario.sentStart)),
                                        )
                                    } else {
                                        dateTimeWithTimeZone("Date Prop", scenario.sentStart, scenario.sentTimeZone)
                                    }
                                }
                            }
                    }
                delay(1000.milliseconds)

                // ── Read back ─────────────────────────────────────────────────
                val observed =
                    createdPages.map { (scenario, page) ->
                        val read = notion.pages.retrieve(page.id)
                        scenario to (read.properties["Date Prop"] as? PageProperty.Date)?.date
                    }

                /** Normalised trailing offset of a Notion datetime string, or a description of its absence. */
                fun offsetOf(value: String?): String =
                    when {
                        value == null -> "(no value)"

                        !value.contains('T') -> "(date only)"

                        value.endsWith("Z") -> "+00:00"

                        value.length >= 6 &&
                            value[value.length - 3] == ':' &&
                            (value[value.length - 6] == '+' || value[value.length - 6] == '-') -> value.takeLast(6)

                        else -> "(none — returned without an offset)"
                    }

                fun instantOf(value: String?): Instant? = value?.let { runCatching { Instant.parse(it) }.getOrNull() }

                /**
                 * What the IANA tz database (via java.time) makes of the same local time,
                 * for comparison with Notion's answer. N3-N6 confirmed that the two agree,
                 * overlap and gap resolution included.
                 */
                fun ianaReference(scenario: Scenario): String? {
                    val zone = scenario.sentTimeZone?.let { TimeZone.of(it) } ?: return null
                    val local = runCatching { LocalDateTime.parse(scenario.sentStart) }.getOrNull() ?: return null
                    val instant = local.toInstant(zone)
                    return "$instant (offset ${zone.offsetAt(instant)})"
                }

                fun matches(
                    scenario: Scenario,
                    actual: DateData?,
                ): Boolean =
                    actual != null &&
                        actual.start.take(19) == scenario.expectedLocal &&
                        offsetOf(actual.start) == scenario.expectedOffset &&
                        instantOf(actual.start) == scenario.expectedInstant

                fun passOrFail(match: Boolean) = if (match) "✅" else "❌ — record this"

                // ── Report on container page ──────────────────────────────────
                notion.blocks.appendChildren(containerPageId) {
                    heading2("Part 3: Naive Datetimes & DST Boundaries")
                    paragraph(
                        "Behaviour recorded against the live API on 2026-08-21. ❌ means Notion no " +
                            "longer does what the test recorded — the new behaviour needs writing into " +
                            "the test's KDoc.",
                    )
                    observed.forEach { (scenario, actual) ->
                        paragraph {
                            bold("${scenario.key}: ${scenario.label}")
                            text("  ")
                            bold("sent:")
                            text(" start=\"${scenario.sentStart}\", time_zone=${scenario.sentTimeZone ?: "(omitted)"}  ")
                            bold("expected:")
                            text(" ${scenario.expectedLocal}${scenario.expectedOffset} = ${scenario.expectedInstant}  ")
                            bold("got:")
                            text(
                                " start=\"${actual?.start}\", time_zone=${actual?.timeZone} " +
                                    "→ offset ${offsetOf(actual?.start)}, instant ${instantOf(actual?.start) ?: "unparseable"}  " +
                                    "${passOrFail(matches(scenario, actual))}",
                            )
                            ianaReference(scenario)?.let {
                                text("  ")
                                italic("IANA/java.time for the same local time: $it")
                            }
                        }
                    }
                }

                // ── Console record ────────────────────────────────────────────
                println("\n=== Part 3: naive datetimes & DST boundaries — FINDINGS TO RECORD ===")
                observed.forEach { (scenario, actual) ->
                    println("${scenario.key}  ${scenario.label}")
                    println("    sent      start=\"${scenario.sentStart}\"  time_zone=${scenario.sentTimeZone ?: "(omitted)"}")
                    println("    got       start=\"${actual?.start}\"  time_zone=${actual?.timeZone}")
                    println("    →         offset=${offsetOf(actual?.start)}  instant=${instantOf(actual?.start) ?: "unparseable"}")
                    println(
                        "    expected  ${scenario.expectedLocal}${scenario.expectedOffset} = ${scenario.expectedInstant}  " +
                            passOrFail(matches(scenario, actual)),
                    )
                    ianaReference(scenario)?.let { println("    IANA      $it") }
                }
                println("=== end of findings ===\n")

                // ── Assertions ────────────────────────────────────────────────
                // Soft assertions on purpose: one live run should report every
                // scenario's verdict, not stop at the first surprise.
                assertSoftly {
                    observed.forEach { (scenario, actual) ->
                        withClue(
                            "${scenario.key} (${scenario.label}) — " +
                                "sent start=\"${scenario.sentStart}\", time_zone=${scenario.sentTimeZone ?: "(omitted)"}; " +
                                "Notion returned start=\"${actual?.start}\", time_zone=${actual?.timeZone} " +
                                "(offset ${offsetOf(actual?.start)}, instant ${instantOf(actual?.start) ?: "unparseable"}). " +
                                "Recorded 2026-08-21: ${scenario.finding} " +
                                "If this fails, Notion's behaviour has changed — write what it does now into " +
                                "the KDoc \"Key findings\" block before changing the expectation.",
                        ) {
                            actual shouldNotBe null
                            actual?.start?.take(19) shouldBe scenario.expectedLocal
                            offsetOf(actual?.start) shouldBe scenario.expectedOffset
                            instantOf(actual?.start) shouldBe scenario.expectedInstant
                            // Consistent with Parts 1 and 2: Notion never echoes a named zone.
                            actual?.timeZone shouldBe null
                        }
                    }
                }

                println("✅ Naive datetime & DST boundary scenarios matched the recorded behaviour")
            }
        }
    })
