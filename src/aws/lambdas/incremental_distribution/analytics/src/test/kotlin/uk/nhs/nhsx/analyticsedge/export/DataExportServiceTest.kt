package uk.nhs.nhsx.analyticsedge.export

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.analyticsedge.Dataset
import uk.nhs.nhsx.analyticsedge.QueryErrorEvent
import uk.nhs.nhsx.analyticsedge.QueryFinishedEvent
import uk.nhs.nhsx.analyticsedge.QueryId
import uk.nhs.nhsx.analyticsedge.QueryResult
import uk.nhs.nhsx.analyticsedge.QueryStillRunning
import uk.nhs.nhsx.analyticsedge.QueueClient
import uk.nhs.nhsx.analyticsedge.QueueMessage
import uk.nhs.nhsx.analyticsedge.datasets.AnalyticsSource
import uk.nhs.nhsx.core.ContentType
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents

class DataExportServiceTest {

    private val locatorSlot = slot<Locator>()
    private val contentTypeSlot = slot<ContentType>()
    private val bytesSlot = slot<ByteArraySource>()

    private val fromSlot = slot<Locator>()
    private val toSlot = slot<Locator>()

    private val s3Storage = mockk<AwsS3> {
        every { upload(capture(locatorSlot), capture(contentTypeSlot), capture(bytesSlot)) } just Runs
        every { copyObject(capture(fromSlot), capture(toSlot)) } just Runs
    }
    private val exportBucketName = BucketName.of("export-bucket")
    private val athenaOutputBucketName = BucketName.of("athena-output-bucket")
    private val events = RecordingEvents()
    private val queueClient = mockk<QueueClient> {
        every { sendMessage(any()) } just Runs
    }

    private fun queueMessage(queryId: String, dataset: Dataset) = QueueMessage(QueryId(queryId), dataset)

    private fun allFinishedSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Finished(Unit)
        override fun startAdoptionDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAggregateDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startEnpicDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startIsolationDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startPosterDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allWaitingSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Waiting()
        override fun startAdoptionDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAggregateDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startEnpicDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startIsolationDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startPosterDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allFailedSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Error("some error")
        override fun startAdoptionDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAggregateDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startEnpicDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startIsolationDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startPosterDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    @Test
    fun `triggers all queries`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allWaitingSource("test"),
            queueClient,
            events
        )
        service.triggerAllQueries()

        verify(exactly = 5) { queueClient.sendMessage(any()) }
    }

    @Disabled
    @Test
    fun `finished adoption export is uploaded`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFinishedSource("adoption"),
            queueClient,
            events
        )
        service.export(queueMessage("adoption", Dataset.Adoption))

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(locatorSlot.captured.bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot.captured.key).isEqualTo(ObjectKey.of("app_adoption.csv"))
        assertThat(contentTypeSlot.captured.value).isEqualTo("text/csv")
        assertThat(String(bytesSlot.captured.toArray())).contains(""""2021-01-01",York,Android,1000""")

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Disabled
    @Test
    fun `waiting adoption export is re-scheduled`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allWaitingSource("adoption"),
            queueClient,
            events
        )
        service.export(queueMessage("adoption", Dataset.Adoption))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("adoption"), Dataset.Adoption)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Disabled
    @Test
    fun `failed adoption export`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFailedSource("adoption"),
            queueClient,
            events
        )
        service.export(queueMessage("adoption", Dataset.Adoption))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Test
    fun `finished aggregate export is uploaded`() {
        val queryId = "aggregate"
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFinishedSource(queryId),
            queueClient,
            events
        )
        service.export(queueMessage(queryId, Dataset.Aggregate))

        verify(exactly = 1) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(fromSlot.captured.bucket).isEqualTo(BucketName.of("athena-output-bucket"))
        assertThat(fromSlot.captured.key).isEqualTo(ObjectKey.of("$queryId.csv"))
        assertThat(toSlot.captured.bucket).isEqualTo(BucketName.of("export-bucket"))
        assertThat(toSlot.captured.key).isEqualTo(ObjectKey.of("app_aggregate.csv"))

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting aggregate export is re-scheduled`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allWaitingSource("aggregate"),
            queueClient,
            events
        )
        service.export(queueMessage("aggregate", Dataset.Aggregate))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("aggregate"), Dataset.Aggregate)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed aggregate export`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFailedSource("aggregate"),
            queueClient,
            events
        )
        service.export(queueMessage("aggregate", Dataset.Aggregate))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Test
    fun `finished enpic export is uploaded`() {
        val queryId = "enpic"
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFinishedSource(queryId),
            queueClient,
            events
        )
        service.export(queueMessage(queryId, Dataset.Enpic))

        verify(exactly = 1) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(fromSlot.captured.bucket).isEqualTo(BucketName.of("athena-output-bucket"))
        assertThat(fromSlot.captured.key).isEqualTo(ObjectKey.of("$queryId.csv"))
        assertThat(toSlot.captured.bucket).isEqualTo(BucketName.of("export-bucket"))
        assertThat(toSlot.captured.key).isEqualTo(ObjectKey.of("app_enpic.csv"))

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting enpic export is re-scheduled`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allWaitingSource("enpic"),
            queueClient,
            events
        )
        service.export(queueMessage("enpic", Dataset.Enpic))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("enpic"), Dataset.Enpic)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed enpic export`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFailedSource("enpic"),
            queueClient,
            events
        )
        service.export(queueMessage("enpic", Dataset.Enpic))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Test
    fun `finished isolation export is uploaded`() {
        val queryId = "isolation"
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFinishedSource(queryId),
            queueClient,
            events
        )
        service.export(queueMessage(queryId, Dataset.Isolation))

        verify(exactly = 1) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(fromSlot.captured.bucket).isEqualTo(BucketName.of("athena-output-bucket"))
        assertThat(fromSlot.captured.key).isEqualTo(ObjectKey.of("$queryId.csv"))
        assertThat(toSlot.captured.bucket).isEqualTo(BucketName.of("export-bucket"))
        assertThat(toSlot.captured.key).isEqualTo(ObjectKey.of("app_isolation.csv"))

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting isolation export is re-scheduled`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allWaitingSource("isolation"),
            queueClient,
            events
        )
        service.export(queueMessage("isolation", Dataset.Isolation))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("isolation"), Dataset.Isolation)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed isolation export`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFailedSource("isolation"),
            queueClient,
            events
        )
        service.export(queueMessage("isolation", Dataset.Isolation))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Disabled
    @Test
    fun `finished poster export is uploaded`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFinishedSource("poster"),
            queueClient,
            events
        )
        service.export(queueMessage("poster", Dataset.Poster))

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(locatorSlot.captured.bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot.captured.key).isEqualTo(ObjectKey.of("app_posters.csv"))
        assertThat(contentTypeSlot.captured.value).isEqualTo("text/csv")
        assertThat(String(bytesSlot.captured.toArray())).contains(""""2021-01-01",Supermarket,48 Woodpecker,,,Durham,DH1 7KD,County Durham,County Durham,NE,England,Restaurants, cafes, pubs and bars""")

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Disabled
    @Test
    fun `waiting poster export is re-scheduled`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allWaitingSource("poster"),
            queueClient,
            events
        )
        service.export(queueMessage("poster", Dataset.Poster))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("poster"), Dataset.Poster)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Disabled
    @Test
    fun `failed poster export`() {
        val service = DataExportService(
            exportBucketName,
            athenaOutputBucketName,
            s3Storage,
            allFailedSource("poster"),
            queueClient,
            events
        )
        service.export(queueMessage("poster", Dataset.Poster))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

}
