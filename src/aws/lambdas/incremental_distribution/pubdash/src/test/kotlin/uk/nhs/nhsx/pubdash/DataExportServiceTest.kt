package uk.nhs.nhsx.pubdash

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource

class DataExportServiceTest {

    private val s3Storage = mockk<AwsS3> { every { copyObject(any(), any()) } just Runs }
    private val exportBucket = BucketName.of("export-bucket")
    private val athenaOutputBucket = BucketName.of("athena-output-bucket")
    private val events = RecordingEvents()
    private val queueClient = mockk<QueueClient> { every { sendMessage(any()) } just Runs }

    private fun allFinishedSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Finished(Unit)
        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allWaitingSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Waiting()
        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allFailedSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Error("some error")
        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun queueMessage(queryId: String, dataset: Dataset) = QueueMessage(QueryId(queryId), dataset)

    @Test
    fun `finished agnostic export is copied to data bucket`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFinishedSource("agnostic"),
            queueClient,
            events
        )
        service.export(queueMessage("agnostic", Dataset.Agnostic))

        verify(exactly = 1) {
            s3Storage.copyObject(any(), any())
        }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        val from = Locator.of(athenaOutputBucket, ObjectKey.of("agnostic.csv"))
        val to = Locator.of(exportBucket, ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv"))
        verify(exactly = 1) { s3Storage.copyObject(from, to) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting agnostic export is re-scheduled`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allWaitingSource("agnostic"),
            queueClient,
            events
        )
        service.export(queueMessage("agnostic", Dataset.Agnostic))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("agnostic"), Dataset.Agnostic)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed agnostic export`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFailedSource("agnostic"),
            queueClient,
            events
        )
        service.export(queueMessage("agnostic", Dataset.Agnostic))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Test
    fun `finished country export is copied to data bucket`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFinishedSource("country"),
            queueClient,
            events
        )
        service.export(queueMessage("country", Dataset.Country))

        val from = Locator.of(athenaOutputBucket, ObjectKey.of("country.csv"))
        val to = Locator.of(exportBucket, ObjectKey.of("data/covid19_app_country_specific_dataset.csv"))
        verify(exactly = 1) { s3Storage.copyObject(from, to) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting country export is re-scheduled`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allWaitingSource("country"),
            queueClient,
            events
        )
        service.export(queueMessage("country", Dataset.Country))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("country"), Dataset.Country)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed country export`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFailedSource("country"),
            queueClient,
            events
        )
        service.export(queueMessage("country", Dataset.Country))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Test
    fun `finished local authority export is copied to data bucket`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFinishedSource("local-auth"),
            queueClient,
            events
        )
        service.export(queueMessage("local-auth", Dataset.LocalAuthority))

        val from = Locator.of(athenaOutputBucket, ObjectKey.of("local-auth.csv"))
        val to = Locator.of(exportBucket, ObjectKey.of("data/covid19_app_data_by_local_authority.csv"))
        verify(exactly = 1) { s3Storage.copyObject(from, to) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting local authority export is re-scheduled`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allWaitingSource("local-auth"),
            queueClient,
            events
        )
        service.export(queueMessage("local-auth", Dataset.LocalAuthority))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("local-auth"), Dataset.LocalAuthority)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed local authority export`() {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFailedSource("local-auth"),
            queueClient,
            events
        )
        service.export(queueMessage("local-auth", Dataset.LocalAuthority))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }
}
