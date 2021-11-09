package uk.nhs.nhsx.pubdash

import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource
import uk.nhs.nhsx.testhelper.assertions.containsExactly

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
        override fun startAppUsageDataByLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAppUsageDataByCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allWaitingSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Waiting()
        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAppUsageDataByLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAppUsageDataByCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun allFailedSource(queryId: String) = object : AnalyticsSource {
        override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = QueryResult.Error("some error")
        override fun startAgnosticDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAppUsageDataByLocalAuthorityDatasetQueryAsync(): QueryId = QueryId(queryId)
        override fun startAppUsageDataByCountryDatasetQueryAsync(): QueryId = QueryId(queryId)
    }

    private fun queueMessage(queryId: String, dataset: Dataset) = QueueMessage(QueryId(queryId), dataset)

    @Test
    fun `triggers all queries`() {
        val analyticsSource = mockk<AnalyticsSource>(relaxed = true)

        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            analyticsSource,
            queueClient,
            events
        )
        service.triggerAllQueries()

        verifyOrder {
            analyticsSource.startAgnosticDatasetQueryAsync()
            analyticsSource.startCountryDatasetQueryAsync()
            analyticsSource.startLocalAuthorityDatasetQueryAsync()
            analyticsSource.startAppUsageDataByLocalAuthorityDatasetQueryAsync()
            analyticsSource.startAppUsageDataByCountryDatasetQueryAsync()
        }
        confirmVerified(analyticsSource)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "agnostic,Agnostic,data/covid19_app_country_agnostic_dataset.csv",
            "country,Country,data/covid19_app_country_specific_dataset.csv",
            "local-auth,LocalAuthority,data/covid19_app_data_by_local_authority.csv",
            "app-usage-local-auth,AppUsageDataByLocalAuthority,data/covid19_app_usage_data_by_local_authority.csv",
            "app-usage-country-auth,AppUsageDataByCountry,data/covid19_app_usage_data_by_country.csv"
        ]
    )
    fun `finished export is copied to data bucket`(queryId: String, dataset: Dataset, csvFileTo: String) {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFinishedSource(queryId),
            queueClient,
            events
        )
        service.export(queueMessage(queryId, dataset))

        val from = Locator.of(athenaOutputBucket, ObjectKey.of("$queryId.csv"))
        val to = Locator.of(exportBucket, ObjectKey.of(csvFileTo))
        verify(exactly = 1) { s3Storage.copyObject(from, to) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }
        confirmVerified(s3Storage)

        expectThat(events).containsExactly(QueryFinishedEvent::class)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "agnostic,Agnostic",
            "country,Country",
            "local-auth,LocalAuthority",
            "app-usage-local-auth,AppUsageDataByLocalAuthority",
            "app-usage-country-auth,AppUsageDataByCountry"
        ]
    )
    fun `waiting export is re-scheduled`(queryId: String, dataset: Dataset) {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allWaitingSource(queryId),
            queueClient,
            events
        )
        service.export(queueMessage(queryId, dataset))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 1) { queueClient.sendMessage(QueueMessage(QueryId(queryId), dataset)) }

        expectThat(events).containsExactly(QueryStillRunning::class)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "agnostic,Agnostic",
            "country,Country",
            "local-auth,LocalAuthority",
            "app-usage-local-auth,AppUsageDataByLocalAuthority",
            "app-usage-country-auth,AppUsageDataByCountry"
        ]
    )
    fun `failed export`(queryId: String, dataset: Dataset) {
        val service = DataExportService(
            exportBucket,
            athenaOutputBucket,
            s3Storage,
            allFailedSource(queryId),
            queueClient,
            events
        )
        service.export(queueMessage(queryId, dataset))

        verify(exactly = 0) { s3Storage.copyObject(any(), any()) }
        verify(exactly = 0) { queueClient.sendMessage(any()) }

        expectThat(events).containsExactly(QueryErrorEvent::class)
    }

}
