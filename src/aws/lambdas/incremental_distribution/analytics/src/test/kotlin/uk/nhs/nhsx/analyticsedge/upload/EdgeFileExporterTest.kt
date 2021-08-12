package uk.nhs.nhsx.analyticsedge.upload

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message
import uk.nhs.nhsx.analyticsedge.DataUploadSkipped
import uk.nhs.nhsx.analyticsedge.DataUploadedToEdge

class EdgeFileExporterTest {

    private val workspace = "te-some-env"

    @Test
    fun `upload file is successful`() {
        val uploader = mockk<EdgeUploader> {
            every { uploadFile(any(), any(), any()) } just Runs
        }

        val exporter = EdgeFileExporter(workspace, FakeS3().withObject("Poster/TEST.csv"), uploader)

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "Poster/TEST.csv" }""" }
            )
        }

        val result = exporter.export(sqsEvent)

        verify { uploader.uploadFile("te-some-env-app-posters.csv", any(), any()) }

        expectThat(result).isA<DataUploadedToEdge>()
    }

    @Test
    fun `ignores unknown files`() {
        val exporter = EdgeFileExporter(workspace, FakeS3().withObject("unknown_prefix/TEST.csv"), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "unknown_prefix/TEST.csv" }""" }
            )
        }

        expectCatching { exporter.export(sqsEvent) }
            .isFailure()
            .message.isNotNull().contains("No enum constant uk.nhs.nhsx.analyticsedge.Dataset.unknown_prefix")
    }

    @Test
    fun `ignores metadata file`() {
        val exporter = EdgeFileExporter(workspace, FakeS3(), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "some_prefix/TEST.csv.metadata" }""" }
            )
        }

        val result = exporter.export(sqsEvent)

        expectThat(result).isA<DataUploadSkipped>()
    }

    @Test
    fun `throws when more than 1 record in sqs event`() {
        val exporter = EdgeFileExporter(workspace, FakeS3(), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage(),
                SQSEvent.SQSMessage()
            )
        }

        expectCatching { exporter.export(sqsEvent) }
            .isFailure()
            .message.isNotNull().contains("Event must have batch size of 1")
    }

    @Test
    fun `throws if event body is not deserializable`() {
        val exporter = EdgeFileExporter(workspace, FakeS3(), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "some-random": "json" }""" }
            )
        }

        expectCatching { exporter.export(sqsEvent) }
            .isFailure()
            .message.isNotNull()
            .contains("""SQS message parsing failed (no retry): sqsMessage.id=null, body={ "some-random": "json" }""")
    }
}
