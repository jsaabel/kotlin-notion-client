package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.DateObject
import it.saabel.kotlinnotionclient.models.base.Mention
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.pages.DateData
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
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
 *   This is why dateMention(LocalDateTime, TimeZone) must pass the local time string
 *   directly, not convert to an Instant first.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew integrationTest --tests "*TimezoneIntegrationTest"
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
                            richText("Sent Value", "start=$localDateTimeUtcExpected → as instant, tz=null")
                            dateTime("Date Prop", localDateTimeUtcExpected, TimeZone.UTC)
                        }
                    }

                // ── C: LocalDateTime + named TZ (converted to UTC instant) ────
                // The dateTime() builder converts LocalDateTime+TZ to a UTC instant,
                // so the named timezone is not preserved in the stored value.
                val namedTz = TimeZone.of("America/New_York")
                val localDateTimeNyExpected = LocalDateTime(2026, 6, 15, 14, 30)
                val localDateTimeNyPage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Scenario", "C: LocalDateTime named TZ (builder → UTC instant, tz lost)")
                            richText("Sent Value", "start=$localDateTimeNyExpected converted to UTC instant, tz=null (named TZ not sent)")
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
                // C: named TZ lost, stored as UTC equivalent (NY is UTC-4 in June → 18:30Z)
                val cOk = actualC?.start?.startsWith("2026-06-15T18:30:00") == true && actualC.timeZone == null
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
                        italic("named TZ is converted to UTC instant, so time_zone is never sent")
                        text("  ")
                        bold("sent:")
                        text(" instant, tz=null  ")
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

                // C: builder converts to UTC instant — NY is UTC-4 in June → 18:30 UTC
                actualC?.start?.startsWith("2026-06-15T18:30:00") shouldBe true
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
                // After builder fix: M4 sends local time + tz.id, Notion returns 14:30 with offset
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
                            " sent start=$dt4Local (local, no conversion), tz=${dt4Tz.id}  |  got start=${actualM4?.start}, tz=${actualM4?.timeZone}  ",
                        )
                        text("start: ${passOrFail(m4StartOk)}  tz (Notion drops it): ${passOrFail(m4TzOk)}")
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

                // M4: builder now passes local time + tz.id (no instant conversion)
                // Notion applies the UTC-4 offset to the local time → "2026-06-15T14:30:00.000-04:00"
                actualM4?.start?.startsWith("2026-06-15T14:30:00") shouldBe true
                actualM4?.timeZone shouldBe null

                actualM5?.start?.startsWith("2026-06-15T18:30:00") shouldBe true
                actualM5?.timeZone shouldBe null

                println("✅ Rich text date mention round-trip verified")
            }
        }
    })
