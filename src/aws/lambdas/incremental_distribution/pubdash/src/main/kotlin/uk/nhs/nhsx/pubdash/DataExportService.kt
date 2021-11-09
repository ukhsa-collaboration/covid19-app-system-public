package uk.nhs.nhsx.pubdash

import com.amazonaws.services.athena.AmazonAthenaClient
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.string
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.value
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.pubdash.Dataset.*
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource
import uk.nhs.nhsx.pubdash.persistence.AnalyticsDao
import uk.nhs.nhsx.pubdash.persistence.AthenaAsyncDbClient

class DataExportService(
    private val exportBucketName: BucketName,
    private val athenaOutputBucketName: BucketName,
    private val awsS3: AwsS3,
    private val analyticsSource: AnalyticsSource,
    private val queueClient: QueueClient,
    private val events: Events
) {

    fun triggerAllQueries() {
        sendToSqs(QueueMessage(analyticsSource.startAgnosticDatasetQueryAsync(), Agnostic))
        sendToSqs(QueueMessage(analyticsSource.startCountryDatasetQueryAsync(), Country))
        sendToSqs(QueueMessage(analyticsSource.startLocalAuthorityDatasetQueryAsync(), LocalAuthority))
        sendToSqs(QueueMessage(analyticsSource.startAppUsageDataByLocalAuthorityDatasetQueryAsync(), AppUsageDataByLocalAuthority))
        sendToSqs(QueueMessage(analyticsSource.startAppUsageDataByCountryDatasetQueryAsync(), AppUsageDataByCountry))
    }

    fun export(message: QueueMessage) {
        when (val queryResult = analyticsSource.checkQueryState(message.queryId)) {
            is QueryResult.Finished -> onFinished(message)
            is QueryResult.Error -> onError(message, queryResult.message)
            is QueryResult.Waiting -> onWait(message)
        }
    }

    private fun onFinished(queueMessage: QueueMessage) {
        events(QueryFinishedEvent(queueMessage.queryId, queueMessage.dataset))
        copyFromAthenaOutputBucketIntoExportBucket(queueMessage)
    }

    private fun copyFromAthenaOutputBucketIntoExportBucket(queueMessage: QueueMessage) {
        val from = Locator.of(athenaOutputBucketName, ObjectKey.of("${queueMessage.queryId.id}.csv"))
        val to = Locator.of(exportBucketName, objectKeyFrom(queueMessage.dataset))
        awsS3.copyObject(from, to)
    }

    private fun objectKeyFrom(dataset: Dataset): ObjectKey =
        when (dataset) {
            Agnostic -> ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv")
            Country -> ObjectKey.of("data/covid19_app_country_specific_dataset.csv")
            LocalAuthority -> ObjectKey.of("data/covid19_app_data_by_local_authority.csv")
            AppUsageDataByLocalAuthority -> ObjectKey.of("data/covid19_app_usage_data_by_local_authority.csv")
            AppUsageDataByCountry -> ObjectKey.of("data/covid19_app_usage_data_by_country.csv")
        }

    private fun onError(queueMessage: QueueMessage, message: String) {
        events(QueryErrorEvent(queueMessage.queryId, queueMessage.dataset, message))
    }

    private fun onWait(queueMessage: QueueMessage) {
        events(QueryStillRunning(queueMessage.queryId, queueMessage.dataset))
        sendToSqs(queueMessage)
    }

    private fun sendToSqs(queueMessage: QueueMessage) {
        queueClient.sendMessage(queueMessage)
    }

}

fun dataExportService(
    environment: Environment,
    events: Events
) = DataExportService(
    exportBucketName = environment.access.required(value("export_bucket_name", BucketName)),
    athenaOutputBucketName = environment.access.required(value("athena_output_bucket_name", BucketName)),
    awsS3 = AwsS3Client(events),
    analyticsSource = AnalyticsDao(
        workspace = environment.access.required(Environment.WORKSPACE),
        asyncDbClient = AthenaAsyncDbClient(
            athena = AmazonAthenaClient.builder().build(),
            workgroup = environment.access.required(string("analytics_workgroup"))
        ),
        mobileAnalyticsTable = environment.access.required(string("mobile_analytics_table"))

    ),
    queueClient = QueueClient(
        queueUrl = environment.access.required(string("queue_url")),
        sqsClient = AmazonSQSClientBuilder.defaultClient(),
        events = events
    ),
    events = events
)
