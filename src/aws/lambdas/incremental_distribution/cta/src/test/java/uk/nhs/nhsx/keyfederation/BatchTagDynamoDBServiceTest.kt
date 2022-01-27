package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.amazonaws.services.dynamodbv2.model.ReturnValue.UPDATED_NEW
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult
import uk.nhs.nhsx.testhelper.assertions.contains
import java.time.Instant
import java.time.LocalDate
import java.util.*

class BatchTagDynamoDBServiceTest {

    val events = RecordingEvents()

    @Test
    fun `returns last upload state`() {
        val table = mockk<Table> {
            every { getItem(PrimaryKey("id", "lastUploadState")) } returns Item()
                .withNumber("uploadTimestamp", 0L)
        }

        expectThat(BatchTagDynamoDBService(table, events))
            .get(BatchTagDynamoDBService::lastUploadState)
            .isNotNull()
            .isEqualTo(UploadKeysResult(0L))
    }

    @Test
    fun `last upload state absent`() {
        val table = mockk<Table> {
            every { getItem(PrimaryKey("id", "lastUploadState")) } returns null
        }

        expectThat(BatchTagDynamoDBService(table, events))
            .get(BatchTagDynamoDBService::lastUploadState)
            .isNull()
    }

    @Test
    fun `returns last federation batch`() {
        val batchTag = BatchTag.of(UUID.randomUUID().toString())
        val batchDate = LocalDate.ofEpochDay(0)

        val table = mockk<Table> {
            every { getItem(PrimaryKey("id", "lastDownloadState")) } returns Item()
                .withString("batchTag", batchTag.value)
                .withString("batchDate", batchDate.toString())
        }

        expectThat(BatchTagDynamoDBService(table, events))
            .get(BatchTagDynamoDBService::latestFederationBatch)
            .isNotNull()
            .isEqualTo(FederationBatch(batchTag, batchDate))
    }

    @Test
    fun `last federation batch absent`() {
        val table = mockk<Table> {
            every { getItem(PrimaryKey("id", "lastDownloadState")) } returns null
        }

        expectThat(BatchTagDynamoDBService(table, events))
            .get(BatchTagDynamoDBService::latestFederationBatch)
            .isNull()
    }

    @Test
    fun `update last federation batch`() {
        val batchTag = BatchTag.of(UUID.randomUUID().toString())
        val batchDate = LocalDate.ofEpochDay(0)
        val batch = FederationBatch(batchTag, batchDate)

        val table = mockk<Table> {
            every {
                putItem(
                    Item()
                        .withPrimaryKey("id", "lastDownloadState")
                        .with("batchTag", batchTag.value)
                        .with("batchDate", batchDate.toString())
                )
            } returns PutItemOutcome(PutItemResult())
        }

        BatchTagDynamoDBService(table, events).updateLatestFederationBatch(batch)

        verify(exactly = 1) { table.putItem(any<Item>()) }

        expectThat(events).contains(FederationDownloadStateUpdated::class)
    }

    @Test
    fun `update last upload state`() {
        val uploadTimestamp = Instant.EPOCH

        val table = mockk<Table> {
            every {
                updateItem(match<UpdateItemSpec> {
                    expectThat(it) {
                        get { keyComponents }.containsExactly(KeyAttribute("id", "lastUploadState"))
                        get { updateExpression }.isEqualTo("set uploadTimestamp = :uploadTimestamp")
                        get { returnValues }.isEqualTo(UPDATED_NEW.toString())
                        get { valueMap }.isEqualTo(ValueMap().withLong(":uploadTimestamp", uploadTimestamp.epochSecond))
                    }
                    true
                })
            } returns UpdateItemOutcome(UpdateItemResult())
        }

        BatchTagDynamoDBService(table, events).updateLastUploadState(uploadTimestamp)

        verify(exactly = 1) { table.updateItem(any<UpdateItemSpec>()) }

        expectThat(events).contains(FederationUploadStateUpdated::class)
    }
}
