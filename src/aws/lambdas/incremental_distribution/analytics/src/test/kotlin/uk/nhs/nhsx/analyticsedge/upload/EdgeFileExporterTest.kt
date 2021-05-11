package uk.nhs.nhsx.analyticsedge.upload

import DataUploadSkipped
import DataUploadedToEdge
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EdgeFileExporterTest {

    @Test
    fun `upload file is successful`() {
        val uploader = mockk<EdgeUploader> {
            every { uploadFile(any(), any(), any()) } just Runs
        }

        val exporter = EdgeFileExporter(FakeS3().withObject("Poster/TEST.csv"), uploader)

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "Poster/TEST.csv" }""" }
            )
        }

        val result = exporter.export(sqsEvent)

        verify { uploader.uploadFile("app_posters.csv", any(), any()) }
        assertThat(result).isInstanceOf(DataUploadedToEdge::class.java)
    }

    @Test
    fun `ignores unknown files`() {
        val exporter = EdgeFileExporter(FakeS3().withObject("unknown_prefix/TEST.csv"), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "unknown_prefix/TEST.csv" }""" }
            )
        }

        assertThatThrownBy { exporter.export(sqsEvent) }
            .hasMessage("No enum constant uk.nhs.nhsx.analyticsedge.Dataset.unknown_prefix")
    }

    @Test
    fun `ignores metadata file`() {
        val exporter = EdgeFileExporter(FakeS3(), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "some_prefix/TEST.csv.metadata" }""" }
            )
        }

        val result = exporter.export(sqsEvent)

        assertThat(result).isInstanceOf(DataUploadSkipped::class.java)
    }

    @Test
    fun `throws when more than 1 record in sqs event`() {
        val exporter = EdgeFileExporter(FakeS3(), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage(),
                SQSEvent.SQSMessage()
            )
        }

        assertThatThrownBy { exporter.export(sqsEvent) }
            .hasMessage("Event must have batch size of 1")
    }

    @Test
    fun `throws if event body is not deserializable`() {
        val exporter = EdgeFileExporter(FakeS3(), mockk())

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "some-random": "json" }""" }
            )
        }

        assertThatThrownBy { exporter.export(sqsEvent) }
            .hasMessage("""SQS message parsing failed (no retry): sqsMessage.id=null, body={ "some-random": "json" }""")
    }


}
