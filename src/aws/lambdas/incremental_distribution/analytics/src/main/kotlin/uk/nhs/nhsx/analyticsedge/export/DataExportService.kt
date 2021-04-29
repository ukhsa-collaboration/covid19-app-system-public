package uk.nhs.nhsx.analyticsedge.export

import com.amazonaws.services.athena.AmazonAthenaClient
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import uk.nhs.nhsx.analyticsedge.Dataset
import uk.nhs.nhsx.analyticsedge.Dataset.*
import uk.nhs.nhsx.analyticsedge.QueryErrorEvent
import uk.nhs.nhsx.analyticsedge.QueryFinishedEvent
import uk.nhs.nhsx.analyticsedge.QueryResult
import uk.nhs.nhsx.analyticsedge.QueryStillRunning
import uk.nhs.nhsx.analyticsedge.QueueClient
import uk.nhs.nhsx.analyticsedge.QueueMessage
import uk.nhs.nhsx.analyticsedge.datasets.AnalyticsSource
import uk.nhs.nhsx.analyticsedge.persistence.AnalyticsDao
import uk.nhs.nhsx.analyticsedge.persistence.AthenaAsyncDbClient
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.string
import uk.nhs.nhsx.core.aws.s3.*
import uk.nhs.nhsx.core.events.Events

class DataExportService(
    private val exportBucketName: BucketName,
    private val athenaOutputBucketName: BucketName,
    private val awsS3: AwsS3,
    private val analyticsSource: AnalyticsSource,
    private val queueClient: QueueClient,
    private val events: Events
) {

    fun triggerAllQueries() {
        sendToSqs(QueueMessage(analyticsSource.startAdoptionDatasetQueryAsync(), Adoption))
        sendToSqs(QueueMessage(analyticsSource.startAggregateDatasetQueryAsync(), Aggregate))
        sendToSqs(QueueMessage(analyticsSource.startEnpicDatasetQueryAsync(), Enpic))
        sendToSqs(QueueMessage(analyticsSource.startIsolationDatasetQueryAsync(), Isolation))
        sendToSqs(QueueMessage(analyticsSource.startPosterDatasetQueryAsync(), Poster))
    }

    fun export(queueMessage: QueueMessage) {
        when (val queryResult = analyticsSource.checkQueryState(queueMessage.queryId)) {
            is QueryResult.Finished -> onFinished(queueMessage)
            is QueryResult.Error -> onError(queueMessage, queryResult.message)
            is QueryResult.Waiting -> onWait(queueMessage)
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

    private fun objectKeyFrom(dataset: Dataset) =
        when (dataset) {
            Adoption -> ObjectKey.of("app_adoption.csv")
            Aggregate -> ObjectKey.of("app_aggregate.csv")
            Enpic -> ObjectKey.of("app_enpic.csv")
            Isolation -> ObjectKey.of("app_isolation.csv")
            Poster -> ObjectKey.of("app_posters.csv")
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
    exportBucketName = environment.access.required(
        Environment.EnvironmentKey.value("export_bucket_name", BucketName)
    ),
    athenaOutputBucketName = environment.access.required(
        Environment.EnvironmentKey.value("athena_output_bucket_name", BucketName)
    ),
    awsS3 = AwsS3Client(events),
    analyticsSource = AnalyticsDao(
        workspace = environment.access.required(Environment.WORKSPACE),
        asyncDbClient = AthenaAsyncDbClient(
            athena = AmazonAthenaClient.builder().build(),
            workgroup = environment.access.required(string("analytics_workgroup"))
        )
    ),
    queueClient = QueueClient(
        queueUrl = environment.access.required(string("queue_url")),
        sqsClient = AmazonSQSClientBuilder.defaultClient(),
        events = events
    ),
    events = events
)
