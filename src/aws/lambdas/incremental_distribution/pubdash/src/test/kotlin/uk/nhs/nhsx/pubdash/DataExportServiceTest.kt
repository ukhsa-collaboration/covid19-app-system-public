package uk.nhs.nhsx.pubdash

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.http.Consts
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.pubdash.datasets.AgnosticDataset
import uk.nhs.nhsx.pubdash.datasets.AgnosticDatasetRow
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource
import uk.nhs.nhsx.pubdash.datasets.CountryDataset
import uk.nhs.nhsx.pubdash.datasets.CountryDatasetRow
import uk.nhs.nhsx.pubdash.datasets.LocalAuthorityDataset
import uk.nhs.nhsx.pubdash.datasets.LocalAuthorityDatasetRow
import java.time.LocalDate

class DataExportServiceTest {

    private val locatorSlot = slot<Locator>()
    private val contentTypeSlot = slot<ContentType>()
    private val bytesSlot = slot<ByteArraySource>()
    private val s3Storage = mockk<S3Storage> {
        every { upload(capture(locatorSlot), capture(contentTypeSlot), capture(bytesSlot)) } just Runs
    }
    private val bucketName = BucketName.of("bucket")
    private val events = RecordingEvents()
    private val queueClient = mockk<QueueClient> {
        every { sendMessage(any()) } just Runs
    }

    private fun allFinishedSource(queryId: String) = object : AnalyticsSource {
        override fun agnosticDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Finished(
            AgnosticDataset(listOf(AgnosticDatasetRow(LocalDate.parse("2020-11-01"), 1, 2, 3)))
        )

        override fun countryDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Finished(
            CountryDataset(listOf(CountryDatasetRow(LocalDate.parse("2020-11-02"), "lang 1", "lang 2", 4, 5, 6, 7, 8)))
        )

        override fun localAuthorityDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Finished(
            LocalAuthorityDataset(
                listOf(LocalAuthorityDatasetRow(LocalDate.parse("2020-11-03"), "local-auth", 9, 10, 11, 12, 13))
            )
        )

        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allWaitingSource(queryId: String) = object : AnalyticsSource {
        override fun agnosticDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Waiting()
        override fun countryDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Waiting()
        override fun localAuthorityDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Waiting()
        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allFailedSource(queryId: String) = object : AnalyticsSource {
        override fun agnosticDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Error("some error")
        override fun countryDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Error("some error")
        override fun localAuthorityDataset(queryId: QueryId): QueryResult<CsvS3Object> = QueryResult.Error("some error")
        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun queueMessage(queryId: String, dataset: Dataset) = QueueMessage(QueryId(queryId), dataset)

    @Test
    fun `finished agnostic export is uploaded`() {
        val service = DataExportService(bucketName, s3Storage, allFinishedSource("agnostic"), queueClient, events)
        service.export(queueMessage("agnostic", Dataset.Agnostic))

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(locatorSlot.captured.bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot.captured.key).isEqualTo(ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv"))
        assertThat(contentTypeSlot.captured.charset).isEqualTo(Consts.UTF_8)
        assertThat(contentTypeSlot.captured.mimeType).isEqualTo("text/csv")
        assertThat(String(bytesSlot.captured.toArray())).contains(""""2020-11-01",1,2,3""")

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting agnostic export is re-scheduled`() {
        val service = DataExportService(bucketName, s3Storage, allWaitingSource("agnostic"), queueClient, events)
        service.export(queueMessage("agnostic", Dataset.Agnostic))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("agnostic"), Dataset.Agnostic)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed agnostic export`() {
        val service = DataExportService(bucketName, s3Storage, allFailedSource("agnostic"), queueClient, events)
        service.export(queueMessage("agnostic", Dataset.Agnostic))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Test
    fun `finished country export is uploaded`() {
        val service = DataExportService(bucketName, s3Storage, allFinishedSource("country"), queueClient, events)
        service.export(queueMessage("country", Dataset.Country))

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(locatorSlot.captured.bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot.captured.key).isEqualTo(ObjectKey.of("data/covid19_app_country_specific_dataset.csv"))
        assertThat(contentTypeSlot.captured.charset).isEqualTo(Consts.UTF_8)
        assertThat(contentTypeSlot.captured.mimeType).isEqualTo("text/csv")
        assertThat(String(bytesSlot.captured.toArray())).contains(""""2020-11-02","lang 1","lang 2",4,5,6,7,8""")

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting country export is re-scheduled`() {
        val service = DataExportService(bucketName, s3Storage, allWaitingSource("country"), queueClient, events)
        service.export(queueMessage("country", Dataset.Country))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("country"), Dataset.Country)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed country export`() {
        val service = DataExportService(bucketName, s3Storage, allFailedSource("country"), queueClient, events)
        service.export(queueMessage("country", Dataset.Country))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }

    @Test
    fun `finished local authority export is uploaded`() {
        val service = DataExportService(bucketName, s3Storage, allFinishedSource("local-auth"), queueClient, events)
        service.export(queueMessage("local-auth", Dataset.LocalAuthority))

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        assertThat(locatorSlot.captured.bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot.captured.key).isEqualTo(ObjectKey.of("data/covid19_app_data_by_local_authority.csv"))
        assertThat(contentTypeSlot.captured.charset).isEqualTo(Consts.UTF_8)
        assertThat(contentTypeSlot.captured.mimeType).isEqualTo("text/csv")
        assertThat(String(bytesSlot.captured.toArray())).contains(""""2020-11-03","local-auth",9,10,11,12,13""")

        events.containsExactly(QueryFinishedEvent::class)
    }

    @Test
    fun `waiting local authority export is re-scheduled`() {
        val service = DataExportService(bucketName, s3Storage, allWaitingSource("local-auth"), queueClient, events)
        service.export(queueMessage("local-auth", Dataset.LocalAuthority))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId("local-auth"), Dataset.LocalAuthority)) }

        events.containsExactly(QueryStillRunning::class)
    }

    @Test
    fun `failed local authority export`() {
        val service = DataExportService(bucketName, s3Storage, allFailedSource("local-auth"), queueClient, events)
        service.export(queueMessage("local-auth", Dataset.LocalAuthority))

        verify(exactly = 0) { s3Storage.upload(any(), any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        events.containsExactly(QueryErrorEvent::class)
    }
}
