package uk.nhs.nhsx.diagnosiskeydist

import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.EnvironmentKeys
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.ObjectKeyFilters
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.scheduled.SchedulingHandler
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.UploadToS3KeyDistributor
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import java.time.Instant
import java.util.function.Supplier

/**
 * Scheduling strategy:
 * - cron(47 1,3,5,7,9,11,13,15,17,19,21,23 * * ? *)
 *
 *
 * Dependencies
 * - Submissions bucket: submissions must be deleted >14 days after upload (S3 Lifecycle Policy)
 *
 *
 * Error handling strategy:
 * - Fail fast (e.g. error in one thread -> stop all threads immediately)
 * - Log errors
 * - Error alerting (ERROR entries in log group -> error metric -> error alert)
 */

@Suppress("unused")
class DiagnosisKeyDistributionHandler(
    private val clock: Supplier<Instant>,
    events: Events,
    private val service: DistributionService
) : SchedulingHandler(events) {

    @JvmOverloads
    constructor(
        environment: Environment = Environment.fromSystem(),
        clock: Supplier<Instant> = SystemClock.CLOCK,
        events: Events = PrintingJsonEvents(clock),
        parameters: Parameters = AwsSsmParameters(),
        awsCloudFrontClient: AwsCloudFront = AwsCloudFrontClient(events, AmazonCloudFrontClientBuilder.defaultClient()),
        awsS3Client: AwsS3 = AwsS3Client(events),
        awsKmsClient: AWSKMS = AWSKMSClientBuilder.defaultClient()
    ) : this(
        clock,
        events,
        distributionService(environment, clock, events, parameters, awsCloudFrontClient, awsS3Client, awsKmsClient)
    )

    override fun handler() = Handler<ScheduledEvent, Event> { _, _ ->
        try {
            service.distributeKeys(clock.get()).let { KeysDistributed }
        } catch (e: Exception) {
            throw RuntimeException("Failed: Key distribution batch", e)
        }
    }
}

fun distributionService(
    environment: Environment,
    clock: Supplier<Instant>,
    events: Events,
    parameters: Parameters,
    awsCloudFrontClient: AwsCloudFront,
    awsS3Client: AwsS3,
    awsKmsClient: AWSKMS
): DistributionService {
    val batchProcessingConfig = BatchProcessingConfig.fromEnvironment(environment)

    val allowedPrefixes = environment.access.required(EnvironmentKey.strings("DIAGNOSIS_KEY_SUBMISSION_PREFIXES"))
    val submissionBucket = environment.access.required(EnvironmentKeys.SUBMISSION_BUCKET_NAME)
    val objectKeyFilter = ObjectKeyFilters.batched().withPrefixes(allowedPrefixes)
    val submissionRepository = SubmissionFromS3Repository(awsS3Client, objectKeyFilter, submissionBucket, events, clock)
    val standardSigningFactory = StandardSigningFactory(clock, parameters, awsKmsClient)

    return DistributionService(
        submissionRepository,
        ExposureProtobuf(environment.access.required(EnvironmentKey.string("MOBILE_APP_BUNDLE_ID"))),
        UploadToS3KeyDistributor(
            awsS3Client,
            standardSigningFactory.datedSigner(
                batchProcessingConfig.ssmMetaDataSigningKeyParameterName
            )
        ),
        standardSigningFactory.signContentWithKeyFromParameter(
            batchProcessingConfig.ssmAGSigningKeyParameterName
        ),
        awsCloudFrontClient,
        awsS3Client,
        batchProcessingConfig,
        events,
        clock
    )
}
