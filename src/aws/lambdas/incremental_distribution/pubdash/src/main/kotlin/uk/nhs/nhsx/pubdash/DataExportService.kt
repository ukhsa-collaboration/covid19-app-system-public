package uk.nhs.nhsx.pubdash

import com.amazonaws.services.athena.AmazonAthenaClient
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.apache.http.Consts
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.string
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.value
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.pubdash.Dataset.Agnostic
import uk.nhs.nhsx.pubdash.Dataset.Country
import uk.nhs.nhsx.pubdash.Dataset.LocalAuthority
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource
import uk.nhs.nhsx.pubdash.datasets.FakeSource
import uk.nhs.nhsx.pubdash.persistence.AnalyticsDao
import uk.nhs.nhsx.pubdash.persistence.AthenaAsyncDbClient

class DataExportService(
    private val bucketName: BucketName,
    private val s3Storage: S3Storage,
    private val analyticsSource: AnalyticsSource,
    private val queueClient: QueueClient,
    private val events: Events
) {

    fun triggerAllQueries() {
        sendToSqs(QueueMessage(analyticsSource.startAgnosticDatasetQueryAsync(), Agnostic))
        sendToSqs(QueueMessage(analyticsSource.startCountryDatasetQueryAsync(), Country))
        sendToSqs(QueueMessage(analyticsSource.startLocalAuthorityDatasetQueryAsync(), LocalAuthority))
    }

    fun export(message: QueueMessage) {
        when (message.dataset) {
            Agnostic -> processResult(message, analyticsSource.agnosticDataset(message.queryId))
            Country -> processResult(message, analyticsSource.countryDataset(message.queryId))
            LocalAuthority -> processResult(message, analyticsSource.localAuthorityDataset(message.queryId))
        }
    }

    private fun processResult(queueMessage: QueueMessage, queryResult: QueryResult<CsvS3Object>) {
        when (queryResult) {
            is QueryResult.Finished -> onFinished(queueMessage, queryResult.results)
            is QueryResult.Error -> onError(queueMessage, queryResult.message)
            is QueryResult.Waiting -> onWait(queueMessage)
        }
    }

    private fun onFinished(queueMessage: QueueMessage, dataset: CsvS3Object) {
        events.emit(this::class.java, QueryFinishedEvent(queueMessage.queryId, queueMessage.dataset))
        uploadToS3(dataset)
    }

    private fun onError(queueMessage: QueueMessage, message: String) {
        events.emit(this::class.java, QueryErrorEvent(queueMessage.queryId, queueMessage.dataset, message))
    }

    private fun onWait(queueMessage: QueueMessage) {
        events.emit(this::class.java, QueryStillRunning(queueMessage.queryId, queueMessage.dataset))
        sendToSqs(queueMessage)
    }

    private fun sendToSqs(queueMessage: QueueMessage) {
        queueClient.sendMessage(queueMessage)
    }

    private fun uploadToS3(csvS3Object: CsvS3Object) {
        s3Storage.upload(
            Locator.of(bucketName, csvS3Object.objectKey()),
            ContentType.create("text/csv", Consts.UTF_8),
            ByteArraySource.fromUtf8String(csvS3Object.csv())
        )
    }
}

fun dataExportService(
    environment: Environment,
    events: Events
) = DataExportService(
    bucketName = environment.access.required(value("export_bucket_name", BucketName)),
    s3Storage = AwsS3Client(events),
    analyticsSource = FakeSource(
        source = AnalyticsDao(
            workspace = environment.access.required(Environment.WORKSPACE),
            asyncDbClient = AthenaAsyncDbClient(
                athena = AmazonAthenaClient.builder().build(),
                workgroup = environment.access.required(string("analytics_workgroup"))
            )
        )
    ),
    queueClient = QueueClient(
        queueUrl = environment.access.required(string("queue_url")),
        sqsClient = AmazonSQSClientBuilder.defaultClient(),
        events = events
    ),
    events = events
)
