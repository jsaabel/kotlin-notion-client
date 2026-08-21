package unit.webhooks

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.webhooks.WebhookEvent
import it.saabel.kotlinnotionclient.models.webhooks.WebhookEventType
import it.saabel.kotlinnotionclient.models.webhooks.WebhookVerificationRequest
import unit.util.TestFixtures

/**
 * Deserialization tests for webhook event payloads, using the official sample payloads
 * from https://developers.notion.com/reference/webhooks-events-delivery
 * (stored under src/test/resources/api/webhooks/).
 */
@Tags("Unit")
class WebhookEventModelTest :
    FunSpec({

        context("official sample payloads") {
            test("page.created") {
                val event = WebhookEvent.fromJson(TestFixtures.Webhooks.pageCreatedAsString())
                event.id shouldBe "367cba44-b6f3-4c92-81e7-6a2e9659efd4"
                event.timestamp shouldBe "2024-12-05T23:55:34.285Z"
                event.workspaceId shouldBe "13950b26-c203-4f3b-b97d-93ec06319565"
                event.workspaceName shouldBe "Quantify Labs"
                event.subscriptionId shouldBe "29d75c0d-5546-4414-8459-7b7a92f1fc4b"
                event.integrationId shouldBe "0ef2e755-4912-8096-91c1-00376a88a5ca"
                event.type shouldBe "page.created"
                event.eventType shouldBe WebhookEventType.PAGE_CREATED
                event.attemptNumber shouldBe 1
                event.authors shouldHaveSize 1
                event.authors[0].type shouldBe "person"
                event.accessibleBy shouldHaveSize 2
                event.accessibleBy[1].type shouldBe "bot"
                event.entity.shouldNotBeNull().type shouldBe "page"
                event.data
                    .shouldNotBeNull()
                    .parent
                    .shouldNotBeNull()
                    .type shouldBe "page"
            }

            test("page.content_updated carries updated_blocks") {
                val event = WebhookEvent.fromJson(TestFixtures.Webhooks.pageContentUpdatedAsString())
                event.eventType shouldBe WebhookEventType.PAGE_CONTENT_UPDATED
                val data = event.data.shouldNotBeNull()
                data.updatedBlocks shouldHaveSize 1
                data.updatedBlocks[0].id shouldBe "153104cd-477e-80ec-a87d-f7ff0236d35c"
                data.updatedBlocks[0].type shouldBe "block"
            }

            test("page.properties_updated exposes updated property ids (string array shape)") {
                val event = WebhookEvent.fromJson(TestFixtures.Webhooks.pagePropertiesUpdatedAsString())
                event.eventType shouldBe WebhookEventType.PAGE_PROPERTIES_UPDATED
                val data = event.data.shouldNotBeNull()
                data.parent.shouldNotBeNull().type shouldBe "space"
                data.updatedPropertyIds shouldBe listOf("XGe%40", "bDf%5B", "DbAu")
                // The other accessor must not misinterpret the string-array shape.
                data.updatedSchemaProperties shouldBe emptyList()
            }

            test("database.schema_updated exposes schema changes (object array shape)") {
                val event = WebhookEvent.fromJson(TestFixtures.Webhooks.databaseSchemaUpdatedAsString())
                event.eventType shouldBe WebhookEventType.DATABASE_SCHEMA_UPDATED
                val data = event.data.shouldNotBeNull()
                val changes = data.updatedSchemaProperties
                changes shouldHaveSize 3
                changes[0].id shouldBe "kqLW"
                changes[0].name shouldBe "Created at"
                changes[0].action shouldBe "created"
                changes[2].action shouldBe "deleted"
                // And the string accessor must be empty for this shape.
                data.updatedPropertyIds shouldBe emptyList()
            }

            test("data_source.created (2025-09-03 shape: api_version, no accessible_by)") {
                val event = WebhookEvent.fromJson(TestFixtures.Webhooks.dataSourceCreatedAsString())
                event.eventType shouldBe WebhookEventType.DATA_SOURCE_CREATED
                event.apiVersion shouldBe "2025-09-03"
                event.accessibleBy shouldBe emptyList()
                event.entity.shouldNotBeNull().type shouldBe "data_source"
            }

            test("comment.created carries page_id") {
                val event = WebhookEvent.fromJson(TestFixtures.Webhooks.commentCreatedAsString())
                event.eventType shouldBe WebhookEventType.COMMENT_CREATED
                event.entity.shouldNotBeNull().type shouldBe "comment"
                val data = event.data.shouldNotBeNull()
                data.pageId shouldBe "0ef104cd-477e-80e1-8571-cfd10e92339a"
                data.parent.shouldNotBeNull().type shouldBe "page"
            }
        }

        context("forward compatibility") {
            test("unknown event type deserializes and maps to UNKNOWN, raw string preserved") {
                val raw =
                    TestFixtures.Webhooks
                        .pageCreatedAsString()
                        .replace("page.created", "page.telepathically_updated")
                val event = WebhookEvent.fromJson(raw)
                event.type shouldBe "page.telepathically_updated"
                event.eventType shouldBe WebhookEventType.UNKNOWN
            }

            test("unknown top-level and data keys are ignored") {
                val raw =
                    TestFixtures.Webhooks
                        .pageCreatedAsString()
                        .replace("\"attempt_number\": 1,", "\"attempt_number\": 1, \"brand_new_field\": {\"x\": 1},")
                val event = WebhookEvent.fromJson(raw)
                event.eventType shouldBe WebhookEventType.PAGE_CREATED
            }

            test("unknown entity and principal types stay readable as raw strings") {
                val raw =
                    TestFixtures.Webhooks
                        .pageCreatedAsString()
                        .replace("\"type\": \"person\"", "\"type\": \"agent\"")
                val event = WebhookEvent.fromJson(raw)
                event.authors[0].type shouldBe "agent"
            }
        }

        context("verification request") {
            test("parses the one-time verification payload") {
                val request = WebhookVerificationRequest.fromJsonOrNull(TestFixtures.Webhooks.verificationRequestAsString())
                request.shouldNotBeNull().verificationToken shouldBe
                    "secret_tMrlL1qK5vuQAh1b6cZGhFChZTSYJlce98V0pYn7yBl"
            }

            test("returns null for a regular event payload") {
                WebhookVerificationRequest.fromJsonOrNull(TestFixtures.Webhooks.pageCreatedAsString()) shouldBe null
            }

            test("returns null for invalid JSON") {
                WebhookVerificationRequest.fromJsonOrNull("not json at all") shouldBe null
            }
        }
    })
