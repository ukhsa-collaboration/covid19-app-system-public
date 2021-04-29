package uk.nhs.nhsx.analyticsedge.export

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.TestEnvironments.environmentWith
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext

class DataExportHandlerTest {

    private val service = mockk<DataExportService> {
        every { export(any()) } just Runs
    }

    private fun sqsBodyFor(dataset: String) = """{"queryId":{"id": "1"},"dataset":"$dataset"}"""

    @Test
    fun `succeeds for single record - adoption dataset`() {
        val singleRecordSqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = sqsBodyFor("Adoption") }
            )
        }

        val handler = DataExportHandler(environment = environmentWith(), service = service)
        val response = handler.handleRequest(singleRecordSqsEvent, TestContext())

        assertThat(response).isEqualTo(DataExportHandled.toString())
        verify(exactly = 1) { service.export(any()) }
    }

    @Test
    fun `succeeds for single record - aggregate dataset`() {
        val singleRecordSqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = sqsBodyFor("Aggregate") }
            )
        }

        val handler = DataExportHandler(environment = environmentWith(), service = service)
        val response = handler.handleRequest(singleRecordSqsEvent, TestContext())

        assertThat(response).isEqualTo(DataExportHandled.toString())
        verify(exactly = 1) { service.export(any()) }
    }

    @Test
    fun `succeeds for single record - enpic dataset`() {
        val singleRecordSqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = sqsBodyFor("Enpic") }
            )
        }

        val handler = DataExportHandler(environment = environmentWith(), service = service)
        val response = handler.handleRequest(singleRecordSqsEvent, TestContext())

        assertThat(response).isEqualTo(DataExportHandled.toString())
        verify(exactly = 1) { service.export(any()) }
    }

    @Test
    fun `succeeds for single record - isolation dataset`() {
        val singleRecordSqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = sqsBodyFor("Isolation") }
            )
        }

        val handler = DataExportHandler(environment = environmentWith(), service = service)
        val response = handler.handleRequest(singleRecordSqsEvent, TestContext())

        assertThat(response).isEqualTo(DataExportHandled.toString())
        verify(exactly = 1) { service.export(any()) }
    }

    @Test
    fun `succeeds for single record - poster dataset`() {
        val singleRecordSqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = sqsBodyFor("Poster") }
            )
        }

        val handler = DataExportHandler(environment = environmentWith(), service = service)
        val response = handler.handleRequest(singleRecordSqsEvent, TestContext())

        assertThat(response).isEqualTo(DataExportHandled.toString())
        verify(exactly = 1) { service.export(any()) }
    }

    @Test
    fun `returns error event when empty records`() {
        val emptyRecordsSqsEvent = SQSEvent().apply { records = emptyList() }

        val handler = DataExportHandler(environment = environmentWith(), service = service)
        val response = handler.handleRequest(emptyRecordsSqsEvent, TestContext())

        assertThat(response).isEqualTo("""DataExportFailed(message=Expecting only 1 record, got: [])""")
        verify(exactly = 0) { service.export(any()) }
    }

    @Test
    fun `returns error event when multiple records`() {
        val emptyRecordsSqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = sqsBodyFor("Adoption") },
                SQSEvent.SQSMessage().apply { body = sqsBodyFor("Aggregate") }
            )
        }

        val handler = DataExportHandler(environment = environmentWith(), service = service)
        val response = handler.handleRequest(emptyRecordsSqsEvent, TestContext())

        assertThat(response).isEqualTo("""DataExportFailed(message=Expecting only 1 record, got: [{body: {"queryId":{"id": "1"},"dataset":"Adoption"},}, {body: {"queryId":{"id": "1"},"dataset":"Aggregate"},}])""")
        verify(exactly = 0) { service.export(any()) }
    }
}
