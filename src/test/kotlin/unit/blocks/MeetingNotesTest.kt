package unit.blocks

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.MeetingNotesContent
import kotlinx.serialization.json.Json

private val RICH_TEXT_TEAM_SYNC =
    """
    {
      "type": "text",
      "text": { "content": "Team Sync", "link": null },
      "annotations": {
        "bold": false, "italic": false, "strikethrough": false,
        "underline": false, "code": false, "color": "default"
      },
      "plain_text": "Team Sync",
      "href": null
    }
    """.trimIndent()

@Tags("Unit")
class MeetingNotesTest :
    FunSpec({
        val json = Json { ignoreUnknownKeys = true }

        context("MeetingNotesContent deserialization") {
            test("deserializes with all fields present") {
                val raw =
                    """
                    {
                      "title": [$RICH_TEXT_TEAM_SYNC],
                      "status": "notes_ready",
                      "children": {
                        "summary_block_id": "a1b2c3d4-5678-9abc-def0-1234567890ab",
                        "notes_block_id": "b2c3d4e5-6789-abcd-ef01-234567890abc",
                        "transcript_block_id": "c3d4e5f6-789a-bcde-f012-34567890abcd"
                      },
                      "calendar_event": {
                        "attendees": ["ee5f0f84-409a-440f-983a-a5315961c6e4"],
                        "start_time": "2026-02-24T10:00:00.000Z",
                        "end_time": "2026-02-24T10:45:00.000Z"
                      },
                      "recording": {
                        "start_time": "2026-02-24T10:00:00.000Z",
                        "end_time": "2026-02-24T10:45:00.000Z"
                      }
                    }
                    """.trimIndent()

                val content = json.decodeFromString<MeetingNotesContent>(raw)

                content.title.shouldNotBeNull() shouldHaveSize 1
                content.title.first().plainText shouldBe "Team Sync"
                content.status shouldBe "notes_ready"
                content.children.shouldNotBeNull()
                content.children.summaryBlockId shouldBe "a1b2c3d4-5678-9abc-def0-1234567890ab"
                content.children.notesBlockId shouldBe "b2c3d4e5-6789-abcd-ef01-234567890abc"
                content.children.transcriptBlockId shouldBe "c3d4e5f6-789a-bcde-f012-34567890abcd"
                content.calendarEvent.shouldNotBeNull()
                content.calendarEvent.attendees shouldBe listOf("ee5f0f84-409a-440f-983a-a5315961c6e4")
                content.calendarEvent.startTime shouldBe "2026-02-24T10:00:00.000Z"
                content.calendarEvent.endTime shouldBe "2026-02-24T10:45:00.000Z"
                content.recording.shouldNotBeNull()
                content.recording.startTime shouldBe "2026-02-24T10:00:00.000Z"
                content.recording.endTime shouldBe "2026-02-24T10:45:00.000Z"
            }

            test("deserializes with all optional fields absent") {
                val raw = """{}"""

                val content = json.decodeFromString<MeetingNotesContent>(raw)

                content.title.shouldBeNull()
                content.status.shouldBeNull()
                content.children.shouldBeNull()
                content.calendarEvent.shouldBeNull()
                content.recording.shouldBeNull()
            }
        }

        context("Block.MeetingNotes deserialization") {
            test("deserializes a full meeting_notes block from JSON") {
                val raw =
                    """
                    {
                      "object": "block",
                      "id": "d7b3c8f4-9e6e-4c1a-b5b8-2c0f4a0c5b8e",
                      "parent": { "type": "page_id", "page_id": "bbbb0000-0000-0000-0000-000000000002" },
                      "created_time": "2026-02-24T10:00:00.000Z",
                      "last_edited_time": "2026-02-24T10:45:00.000Z",
                      "has_children": false,
                      "in_trash": false,
                      "type": "meeting_notes",
                      "meeting_notes": {
                        "title": [$RICH_TEXT_TEAM_SYNC],
                        "status": "notes_ready",
                        "children": {
                          "summary_block_id": "a1b2c3d4-5678-9abc-def0-1234567890ab",
                          "notes_block_id": "b2c3d4e5-6789-abcd-ef01-234567890abc",
                          "transcript_block_id": "c3d4e5f6-789a-bcde-f012-34567890abcd"
                        },
                        "calendar_event": {
                          "attendees": ["ee5f0f84-409a-440f-983a-a5315961c6e4"],
                          "start_time": "2026-02-24T10:00:00.000Z",
                          "end_time": "2026-02-24T10:45:00.000Z"
                        },
                        "recording": {
                          "start_time": "2026-02-24T10:00:00.000Z",
                          "end_time": "2026-02-24T10:45:00.000Z"
                        }
                      }
                    }
                    """.trimIndent()

                val block = json.decodeFromString<Block>(raw)

                val mn = block.shouldBeInstanceOf<Block.MeetingNotes>()
                mn.type shouldBe "meeting_notes"
                mn.id shouldBe "d7b3c8f4-9e6e-4c1a-b5b8-2c0f4a0c5b8e"
                mn.inTrash shouldBe false
                mn.hasChildren shouldBe false
                mn.meetingNotes.status shouldBe "notes_ready"
                mn.meetingNotes.title!!
                    .first()
                    .plainText shouldBe "Team Sync"
                val children = mn.meetingNotes.children.shouldNotBeNull()
                children.summaryBlockId shouldBe "a1b2c3d4-5678-9abc-def0-1234567890ab"
                children.notesBlockId shouldBe "b2c3d4e5-6789-abcd-ef01-234567890abc"
                children.transcriptBlockId shouldBe "c3d4e5f6-789a-bcde-f012-34567890abcd"
                val calendarEvent = mn.meetingNotes.calendarEvent.shouldNotBeNull()
                calendarEvent.attendees shouldBe listOf("ee5f0f84-409a-440f-983a-a5315961c6e4")
                calendarEvent.startTime shouldBe "2026-02-24T10:00:00.000Z"
                mn.meetingNotes.recording
                    .shouldNotBeNull()
                    .endTime shouldBe "2026-02-24T10:45:00.000Z"
            }
        }
    })
